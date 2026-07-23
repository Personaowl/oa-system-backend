package com.personaowl.oa.user.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.common.redis.CacheNames;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;

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
        return listUsersInternal(keyword, departmentId, page, size);
    }

    @Transactional(readOnly = true)
    public UserPageResponse listUsers(Long operatorId, String roles, String keyword,
                                      Long departmentId, Integer page, Integer size) {
        Long scopedDepartmentId = resolveScopedDepartment(operatorId, roles);
        if (scopedDepartmentId != null && departmentId != null && !scopedDepartmentId.equals(departmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "部门主管只能查看本部门员工");
        }
        return listUsersInternal(keyword, scopedDepartmentId == null ? departmentId : scopedDepartmentId, page, size);
    }

    private UserPageResponse listUsersInternal(String keyword, Long departmentId, Integer page, Integer size) {
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
    public UserResponse updateSalary(Long operatorId, String roles, Long userId, BigDecimal salary) {
        if (salary == null || salary.signum() < 0 || salary.scale() > 2
                || salary.precision() - salary.scale() > 10) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "薪资必须是0到9999999999.99之间的金额，最多两位小数");
        }
        SysUser target = requireUser(userId);
        Long scopedDepartmentId = resolveScopedDepartment(operatorId, roles);
        if (scopedDepartmentId != null && !scopedDepartmentId.equals(target.getDepartmentId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "部门主管只能调整本部门员工薪资");
        }
        BigDecimal normalized = salary.setScale(2);
        if (userMapper.updateSalary(userId, normalized) != 1) throw new IllegalStateException("调整薪资失败");
        target.setSalary(normalized);
        return toResponse(target);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.DEPARTMENT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DEPARTMENT_DETAIL, allEntries = true)
    })
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
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.DEPARTMENT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DEPARTMENT_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.USER_ROLES, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.USER_PERMISSIONS, key = "#id")
    })
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
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.DEPARTMENT_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.DEPARTMENT_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.USER_ROLES, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.USER_PERMISSIONS, key = "#id")
    })
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
                user.getPhone(), user.getEmail(), user.getSalary(), user.getStatus(), roleIds, roleCodes);
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

    private Long resolveScopedDepartment(Long operatorId, String rolesHeader) {
        Set<String> roles = parseRoles(rolesHeader);
        if (roles.contains("ADMIN") || roles.contains("HR")) return null;
        if (!roles.contains("MANAGER")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号无权访问员工管理");
        }
        SysUser operator = requireUser(operatorId);
        if (operator.getDepartmentId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "部门主管尚未分配部门");
        }
        SysDepartment department = departmentMapper.findAvailableById(operator.getDepartmentId());
        if (department == null || !operatorId.equals(department.getManagerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号不是所属部门负责人");
        }
        return department.getId();
    }

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return Set.of();
        Set<String> roles = new LinkedHashSet<>();
        Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .forEach(roles::add);
        return roles;
    }
}
