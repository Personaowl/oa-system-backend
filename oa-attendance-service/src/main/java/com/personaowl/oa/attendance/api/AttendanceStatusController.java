package com.personaowl.oa.attendance.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.common.web.PermissionGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/attendance")
@Tag(name = "服务状态", description = "开发期连通性接口；生产健康检查使用 /actuator/health")
public class AttendanceStatusController {
    private final PermissionGuard permissionGuard;

    public AttendanceStatusController(PermissionGuard permissionGuard) {
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/status")
    @Deprecated
    @Operation(summary = "服务连通性检查", description = "已弃用，仅为兼容开发联调保留",
            deprecated = true, security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Map<String, String>> status(
            @Parameter(hidden = true)
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @Parameter(hidden = true)
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "attendance:read");
        return ApiResponse.success(Map.of("service", "oa-attendance-service", "status", "UP"), traceId);
    }
}
