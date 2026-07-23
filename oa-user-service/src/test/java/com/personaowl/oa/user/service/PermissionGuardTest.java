package com.personaowl.oa.user.service;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.web.PermissionGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionGuardTest {
    private final PermissionGuard guard = new PermissionGuard();

    @Test
    void acceptsExactPermissionAndSystemAdmin() {
        assertDoesNotThrow(() -> guard.require("sys:dept:list,notice:view", "sys:dept:list"));
        assertDoesNotThrow(() -> guard.require("system:admin", "sys:dept:delete"));
    }

    @Test
    void rejectsMissingPermission() {
        assertThrows(BusinessException.class, () -> guard.require("notice:view", "sys:dept:list"));
        assertThrows(BusinessException.class, () -> guard.require(null, "sys:dept:list"));
    }
}
