package com.personaowl.oa.user.api.dto;

import java.util.Set;

public record CurrentUserResponse(
        Long id,
        Long departmentId,
        String username,
        String displayName,
        String avatarUrl,
        String phone,
        String email,
        Set<String> roles,
        Set<String> permissions
) {
}
