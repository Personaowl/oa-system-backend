package com.personaowl.oa.user.api.dto;

import java.math.BigDecimal;
import java.util.Set;

public record UserResponse(
        Long id,
        Long departmentId,
        String departmentName,
        String username,
        String displayName,
        String phone,
        String email,
        BigDecimal salary,
        String salaryGrade,
        BigDecimal performanceSalary,
        BigDecimal deductionSalary,
        BigDecimal payableSalary,
        Integer status,
        Set<Long> roleIds,
        Set<String> roleCodes
) {
    public UserResponse(Long id, Long departmentId, String departmentName, String username,
                        String displayName, String phone, String email, BigDecimal salary,
                        Integer status, Set<Long> roleIds, Set<String> roleCodes) {
        this(id, departmentId, departmentName, username, displayName, phone, email, salary,
                "13A", BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                salary == null ? BigDecimal.ZERO.setScale(2) : salary,
                status, roleIds, roleCodes);
    }
}
