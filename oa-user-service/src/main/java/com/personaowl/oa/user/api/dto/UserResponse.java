package com.personaowl.oa.user.api.dto;

import java.util.Set;

public record UserResponse(
        Long id,
        Long departmentId,
        String departmentName,
        String username,
        String displayName,
        String phone,
        String email,
        Integer status,
        Set<Long> roleIds,
        Set<String> roleCodes
) {
}
