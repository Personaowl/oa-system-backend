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
        Integer status,
        Set<Long> roleIds,
        Set<String> roleCodes
) {
}
