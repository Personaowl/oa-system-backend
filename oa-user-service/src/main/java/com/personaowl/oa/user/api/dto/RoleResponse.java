package com.personaowl.oa.user.api.dto;

import com.personaowl.oa.user.domain.SysRole;

import java.util.Set;

public record RoleResponse(
        Long id,
        String code,
        String name,
        Integer status,
        Set<Long> permissionIds) {
    public static RoleResponse from(SysRole role, Set<Long> permissionIds) {
        return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getStatus(), permissionIds);
    }
}
