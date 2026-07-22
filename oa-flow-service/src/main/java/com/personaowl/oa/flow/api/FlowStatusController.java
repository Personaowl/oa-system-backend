package com.personaowl.oa.flow.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.common.web.PermissionGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/flows")
public class FlowStatusController {
    private final PermissionGuard permissionGuard;

    public FlowStatusController(PermissionGuard permissionGuard) {
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, String>> status(
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "flow:read");
        return ApiResponse.success(Map.of("service", "oa-flow-service", "status", "UP"), traceId);
    }
}
