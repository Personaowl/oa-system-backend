package com.personaowl.oa.user.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.common.web.PermissionGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserStatusController {

    private final String applicationName;
    private final PermissionGuard permissionGuard;

    public UserStatusController(@Value("${spring.application.name}") String applicationName,
                                PermissionGuard permissionGuard) {
        this.applicationName = applicationName;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, String>> status(
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "user:read");
        return ApiResponse.success(Map.of("service", applicationName, "status", "UP"), traceId);
    }
}
