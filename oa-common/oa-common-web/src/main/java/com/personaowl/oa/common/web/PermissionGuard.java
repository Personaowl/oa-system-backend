package com.personaowl.oa.common.web;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates the permission context injected by the gateway.
 */
public class PermissionGuard {
    private static final String ADMIN_PERMISSION = "system:admin";

    public void require(String permissionsHeader, String requiredPermission) {
        Set<String> permissions = parse(permissionsHeader);
        if (!permissions.contains(ADMIN_PERMISSION)
                && !permissions.contains(requiredPermission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Set<String> parse(String permissionsHeader) {
        if (permissionsHeader == null || permissionsHeader.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(permissionsHeader.split("[,;\\s]+"))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
