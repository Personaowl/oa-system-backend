package com.personaowl.oa.user.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.user.api.dto.UserCreateRequest;
import com.personaowl.oa.user.api.dto.UserPageResponse;
import com.personaowl.oa.user.api.dto.UserResponse;
import com.personaowl.oa.user.api.dto.UserUpdateRequest;
import com.personaowl.oa.user.domain.SysDepartment;
import com.personaowl.oa.user.domain.SysUser;
import com.personaowl.oa.user.mapper.SysDepartmentMapper;
import com.personaowl.oa.user.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserManagementService {
    private final SysUserMapper userMapper;
    private final SysDepartmentMapper departmentMapper;
    private final RbacService rbacService;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(SysUserMapper userMapper,
                                 SysDepartmentMapper departmentMapper,
                                 RbacService rbacService,
                                 PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.rbacService = rbacService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserPageResponse listUsers(String keyword, Long departmentId, Integer page, Integer size) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.isEmpty()) normalizedKeyword = null;
        if (departmentId != null) requireDepartment(departmentId);
        int normalizedPage = Math.max(1, page == null ? 1 : page);
        int normalizedSize = Math.min(100, Math.max(1, size == null ? 20 : size));
        long total = userMapper.countAvailable(normalizedKeyword, departmentId);
        if (total == 0) return new UserPageResponse(0, List.of());
        long offset = (long) (normalizedPage - 1) * normalizedSize;
        List<UserResponse> records = userMapper.findAvailablePage(
                        normalizedKeyword, departmentId, offset, normalizedSize)
                .stream().map(this::toResponse).toList();
        return new UserPageResponse(total, records);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String username = normalizeRequired(request.username(), "登录账号", 64);
        if (userMapper.countByUsername(username) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "登录账号已存在");
        }
        requireDepartment(request.departmentId());

        SysUser user = new SysUser();
        user.setId(IdWorker.getId());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(normalizeRequired(request.displayName(), "员工姓名", 64));
        user.setDepartmentId(request.departmentId());
        user.setPhone(normalizeOptional(request.phone()));
        user.setEmail(normalizeOptional(request.email()));
        user.setStatus(normalizeStatus(request.status()));
        user.setDeleted(0);
        if (userMapper.insert(user) != 1) throw new IllegalStateException("创建员工失败");
        rbacService.assignUserRoles(user.getId(), request.roleIds());
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        SysUser user = requireUser(id);
        String username = normalizeRequired(request.username(), "登录账号", 64);
        if (userMapper.countByUsernameExcluding(username, id) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "登录账号已存在");
        }
        requireDepartment(request.departmentId());
        user.setUsername(username);
        user.setDisplayName(normalizeRequired(request.displayName(), "员工姓名", 64));
        user.setDepartmentId(request.departmentId());
        user.setPhone(normalizeOptional(request.phone()));
        user.setEmail(normalizeOptional(request.email()));
        user.setStatus(normalizeStatus(request.status()));
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }
        if (userMapper.updateById(user) != 1) throw new IllegalStateException("更新员工失败");
        rbacService.assignUserRoles(id, request.roleIds());
        return toResponse(user);
    }

    @Transactional
    public void deleteUser(Long id, Long operatorId) {
        if (id != null && id.equals(operatorId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "不能删除当前登录账号");
        }
        requireUser(id);
        userMapper.deleteUserRoles(id);
        if (userMapper.softDeleteUser(id) != 1) throw new IllegalStateException("删除员工失败");
    }

    private UserResponse toResponse(SysUser user) {
        SysDepartment department = user.getDepartmentId() == null
                ? null : departmentMapper.findAvailableById(user.getDepartmentId());
        Set<Long> roleIds = new LinkedHashSet<>(userMapper.findRoleIds(user.getId()));
        Set<String> roleCodes = new LinkedHashSet<>(userMapper.findRoleCodes(user.getId()));
        return new UserResponse(user.getId(), user.getDepartmentId(),
                department == null ? null : department.getName(), user.getUsername(), user.getDisplayName(),
                user.getPhone(), user.getEmail(), user.getStatus(), roleIds, roleCodes);
    }

    private SysUser requireUser(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "用户ID必须是正整数");
        SysUser user = userMapper.findAvailableById(id);
        if (user == null) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "用户不存在或已被删除");
        return user;
    }

    private void requireDepartment(Long id) {
        if (id == null || id <= 0 || departmentMapper.findAvailableById(id) == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "部门不存在或已被删除");
        }
    }

    private String normalizeRequired(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "不能为空");
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "长度超限");
        return normalized;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) return 1;
        if (status != 0 && status != 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "用户状态只能为0或1");
        return status;
    }
}
