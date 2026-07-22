package com.personaowl.oa.user.api.dto;

import com.personaowl.oa.user.domain.SysPermission;

public record PermissionResponse(
        Long id,
        Long parentId,
        String code,
        String name,
        String type,
        String path) {
    public static PermissionResponse from(SysPermission permission) {
        return new PermissionResponse(permission.getId(), permission.getParentId(), permission.getCode(),
                permission.getName(), permission.getType(), permission.getPath());
    }
}
