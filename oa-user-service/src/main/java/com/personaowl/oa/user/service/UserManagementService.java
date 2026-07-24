package com.personaowl.oa.user.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.common.redis.CacheNames;
import com.personaowl.oa.user.api.dto.UserCreateRequest;
import com.personaowl.oa.user.api.dto.UserPageResponse;
import com.personaowl.oa.user.api.dto.UserResponse;
import com.personaowl.oa.user.api.dto.UserUpdateRequest;
import com.personaowl.oa.user.api.dto.SalaryDetailUpdateRequest;
import com.personaowl.oa.user.api.dto.SalaryGradeResponse;
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
    private static final List<SalaryGradeResponse> SALARY_GRADES = List.of(
            grade("13A", "4500"), grade("13B", "4800"), grade("13C", "5200"),
            grade("14A", "5600"), grade("14B", "6000"), grade("14C", "6500"),
            grade("15A", "7000"), grade("15B", "7500"), grade("15C", "8000"),
            grade("16A", "8600"), grade("16B", "9200"), grade("16C", "9800"),
            grade("17A", "10500"), grade("17B", "11200"), grade("17C", "12000"),
            grade("18A", "13000"), grade("18B", "14000"), grade("18C", "15000"),
            grade("19A", "16500"), grade("19B", "18000"), grade("19C", "20000"),
            grade("20A", "22000"), grade("20B", "25000"), grade("20C", "28000")
    );
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
    public UserPageResponse listUsers(Long operatorId, String permissions, String keyword,
                                      Long departmentId, Integer page, Integer size) {
        Long scopedDepartmentId = resolveScopedDepartment(operatorId, permissions);
        if (scopedDepartmentId != null && departmentId != null && !scopedDepartmentId.equals(departmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "部门主管只能查看本部门员工");
        }
        return listUsersInternal(keyword, scopedDepartmentId == null ? departmentId : scopedDepartmentId, page, size);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsersForExport(Long operatorId, String permissions, String keyword,
                                                 Long departmentId) {
        Long scopedDepartmentId = resolveScopedDepartment(operatorId, permissions);
        if (scopedDepartmentId != null && departmentId != null && !scopedDepartmentId.equals(departmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "部门主管只能导出本部门员工");
        }
        Long effectiveDepartmentId = scopedDepartmentId == null ? departmentId : scopedDepartmentId;
        if (effectiveDepartmentId != null) requireDepartment(effectiveDepartmentId);
        return userMapper.findAllAvailable(normalizeKeyword(keyword), effectiveDepartmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UserPageResponse listUsersInternal(String keyword, Long departmentId, Integer page, Integer size) {
        String normalizedKeyword = normalizeKeyword(keyword);
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Transactional
    public UserResponse updateSalary(Long operatorId, String permissions, Long userId, BigDecimal salary) {
        if (salary == null || salary.signum() < 0 || salary.scale() > 2
                || salary.precision() - salary.scale() > 10) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "薪资必须是0到9999999999.99之间的金额，最多两位小数");
        }
        SysUser target = requireUser(userId);
        Long scopedDepartmentId = resolveScopedDepartment(operatorId, permissions);
        if (scopedDepartmentId != null && !scopedDepartmentId.equals(target.getDepartmentId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "部门主管只能调整本部门员工薪资");
        }
        BigDecimal normalized = salary.setScale(2);
        if (userMapper.updateSalary(userId, normalized) != 1) throw new IllegalStateException("调整薪资失败");
        target.setSalary(normalized);
        return toResponse(target);
    }

    @Transactional(readOnly = true)
    public List<SalaryGradeResponse> listSalaryGrades() {
        return SALARY_GRADES;
    }

    @Transactional
    public UserResponse updateSalaryDetail(Long operatorId, String permissions, Long userId,
                                           SalaryDetailUpdateRequest request) {
        SysUser target = requireUser(userId);
        Long scopedDepartmentId = resolveScopedDepartment(operatorId, permissions);
        if (scopedDepartmentId != null && !scopedDepartmentId.equals(target.getDepartmentId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "部门主管只能调整本部门员工薪资");
        }

        String gradeCode = request.salaryGrade().trim().toUpperCase(Locale.ROOT);
        BigDecimal baseSalary = SALARY_GRADES.stream()
                .filter(item -> item.code().equals(gradeCode))
                .map(SalaryGradeResponse::baseSalary)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ARGUMENT, "职级必须在13A至20C之间"));
        BigDecimal performanceSalary = normalizeMoney(request.performanceSalary(), "绩效工资");
        BigDecimal deductionSalary = normalizeMoney(request.deductionSalary(), "扣除工资");
        if (deductionSalary.compareTo(baseSalary.add(performanceSalary)) > 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "扣除工资不能超过基础薪资与绩效工资之和");
        }
        if (userMapper.updateSalaryDetail(userId, gradeCode, baseSalary,
                performanceSalary, deductionSalary) != 1) {
            throw new IllegalStateException("更新员工薪资失败");
        }
        target.setSalaryGrade(gradeCode);
        target.setSalary(baseSalary);
        target.setPerformanceSalary(performanceSalary);
        target.setDeductionSalary(deductionSalary);
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
        user.setSalaryGrade("13A");
        user.setSalary(new BigDecimal("4500.00"));
        user.setPerformanceSalary(BigDecimal.ZERO.setScale(2));
        user.setDeductionSalary(BigDecimal.ZERO.setScale(2));
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
                user.getPhone(), user.getEmail(), moneyOrZero(user.getSalary()),
                user.getSalaryGrade() == null ? "13A" : user.getSalaryGrade(),
                moneyOrZero(user.getPerformanceSalary()), moneyOrZero(user.getDeductionSalary()),
                moneyOrZero(user.getSalary()).add(moneyOrZero(user.getPerformanceSalary()))
                        .subtract(moneyOrZero(user.getDeductionSalary())),
                user.getStatus(), roleIds, roleCodes);
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

    private BigDecimal normalizeMoney(BigDecimal value, String label) {
        if (value == null || value.signum() < 0 || value.scale() > 2
                || value.precision() - value.scale() > 10) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    label + "必须是0到9999999999.99之间的金额，最多两位小数");
        }
        return value.setScale(2);
    }

    private BigDecimal moneyOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2);
    }

    private static SalaryGradeResponse grade(String code, String baseSalary) {
        return new SalaryGradeResponse(code, new BigDecimal(baseSalary).setScale(2));
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) return 1;
        if (status != 0 && status != 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "用户状态只能为0或1");
        return status;
    }

    private Long resolveScopedDepartment(Long operatorId, String permissionsHeader) {
        Set<String> permissions = parsePermissions(permissionsHeader);
        if (permissions.contains("SYSTEM:ADMIN") || permissions.contains("DATA:SCOPE:ALL")) return null;
        if (!permissions.contains("DATA:SCOPE:DEPARTMENT")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色未配置员工数据范围");
        }
        SysUser operator = requireUser(operatorId);
        if (operator.getDepartmentId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户尚未分配部门");
        }
        if (departmentMapper.countManagedDepartment(operatorId, operator.getDepartmentId()) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户不是所属部门负责人");
        }
        return operator.getDepartmentId();
    }

    private Set<String> parsePermissions(String permissionsHeader) {
        if (permissionsHeader == null || permissionsHeader.isBlank()) return Set.of();
        Set<String> permissions = new LinkedHashSet<>();
        Arrays.stream(permissionsHeader.split("[,;\\s]+"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .forEach(permissions::add);
        return permissions;
    }
}
