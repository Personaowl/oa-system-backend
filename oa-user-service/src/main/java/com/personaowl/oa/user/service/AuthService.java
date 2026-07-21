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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public AuthService(SysUserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       JwtProperties jwtProperties) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        SysUser user = userMapper.findEnabledByUsername(username);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        Set<String> roles = toStableSet(userMapper.findRoleCodes(user.getId()));
        Set<String> permissions = toStableSet(userMapper.findPermissionCodes(user.getId()));
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
        return toCurrentUser(user, Set.of(), Set.of());
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
                toStableSet(userMapper.findRoleCodes(userId)),
                toStableSet(userMapper.findPermissionCodes(userId)));
    }

    @Transactional
    public CurrentUserResponse updateAccount(Long userId, UpdateAccountRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.findEnabledById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User does not exist or is disabled");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        }

        String username = request.username() == null ? "" : request.username().trim();
        String newPassword = request.newPassword() == null ? "" : request.newPassword();
        if (username.isBlank() && newPassword.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "Enter a new username or password");
        }

        if (!username.isBlank() && !username.equals(user.getUsername())) {
            if (userMapper.countByUsername(username) > 0) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Username is already in use");
            }
            user.setUsername(username);
        }
        if (!newPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        userMapper.updateById(user);
        return toCurrentUser(
                user,
                toStableSet(userMapper.findRoleCodes(userId)),
                toStableSet(userMapper.findPermissionCodes(userId)));
    }

    private CurrentUserResponse toCurrentUser(SysUser user, Set<String> roles, Set<String> permissions) {
        return new CurrentUserResponse(
                user.getId(),
                user.getDepartmentId(),
                user.getUsername(),
                user.getDisplayName(),
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
