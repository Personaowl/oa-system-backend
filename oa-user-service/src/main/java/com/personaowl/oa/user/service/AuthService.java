package com.personaowl.oa.user.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.common.security.JwtClaims;
import com.personaowl.oa.common.security.JwtProperties;
import com.personaowl.oa.common.security.JwtTokenService;
import com.personaowl.oa.user.api.dto.CurrentUserResponse;
import com.personaowl.oa.user.api.dto.LoginRequest;
import com.personaowl.oa.user.api.dto.LoginResponse;
import com.personaowl.oa.user.api.dto.RegisterRequest;
import com.personaowl.oa.user.api.dto.UpdateAccountRequest;
import com.personaowl.oa.user.domain.SysUser;
import com.personaowl.oa.user.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private static final String DEFAULT_REGISTERED_ROLE = "EMPLOYEE";

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final AuthorizationCacheService authorizationCacheService;

    @Autowired
    public AuthService(SysUserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       JwtProperties jwtProperties,
                       AuthorizationCacheService authorizationCacheService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.authorizationCacheService = authorizationCacheService;
    }

    /**
     * Kept for focused unit tests that construct the service without Spring.
     */
    AuthService(SysUserMapper userMapper,
                PasswordEncoder passwordEncoder,
                JwtTokenService jwtTokenService,
                JwtProperties jwtProperties) {
        this(userMapper, passwordEncoder, jwtTokenService, jwtProperties, null);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        SysUser user = userMapper.findEnabledByUsername(username);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        Set<String> roles = toStableSet(findRoleCodes(user.getId()));
        Set<String> permissions = toStableSet(findPermissionCodes(user.getId()));
        String accessToken = jwtTokenService.issue(new JwtClaims(
                user.getId(), user.getUsername(), roles, permissions, null));

        return new LoginResponse(
                "Bearer",
                accessToken,
                jwtProperties.getAccessTokenTtl().toSeconds(),
                toCurrentUser(user, roles, permissions));
    }

    @Transactional
    public CurrentUserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userMapper.countByUsername(username) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Username is already in use");
        }

        SysUser user = new SysUser();
        user.setId(IdWorker.getId());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(username);
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);

        Long roleId = userMapper.findEnabledRoleIdByCode(DEFAULT_REGISTERED_ROLE);
        if (roleId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "默认员工角色未初始化");
        }
        userMapper.insertUserRole(user.getId(), roleId);

        return toCurrentUser(
                user,
                toStableSet(findRoleCodes(user.getId())),
                toStableSet(findPermissionCodes(user.getId())));
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.findEnabledById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已被停用");
        }
        return toCurrentUser(
                user,
                toStableSet(findRoleCodes(userId)),
                toStableSet(findPermissionCodes(userId)));
    }

    @Transactional
    public CurrentUserResponse updateAccount(Long userId, UpdateAccountRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.findEnabledById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已被停用");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "当前密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
        return toCurrentUser(
                user,
                toStableSet(findRoleCodes(userId)),
                toStableSet(findPermissionCodes(userId)));
    }

    private List<String> findRoleCodes(Long userId) {
        return authorizationCacheService == null
                ? userMapper.findRoleCodes(userId)
                : authorizationCacheService.findRoleCodes(userId);
    }

    private List<String> findPermissionCodes(Long userId) {
        return authorizationCacheService == null
                ? userMapper.findPermissionCodes(userId)
                : authorizationCacheService.findPermissionCodes(userId);
    }

    private CurrentUserResponse toCurrentUser(SysUser user, Set<String> roles, Set<String> permissions) {
        return new CurrentUserResponse(
                user.getId(),
                user.getDepartmentId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarFileName() == null ? null : "/api/v1/users/me/avatar",
                user.getPhone(),
                user.getEmail(),
                roles,
                permissions);
    }

    private Set<String> toStableSet(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(new LinkedHashSet<>(values));
    }
}
