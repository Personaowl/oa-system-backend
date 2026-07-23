package com.personaowl.oa.attendance.api;

import com.personaowl.oa.attendance.api.dto.AttendanceScopeResponse;
import com.personaowl.oa.attendance.application.AttendanceScopeService;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceScopeController {

    private final AttendanceScopeService scopeService;

    public AttendanceScopeController(AttendanceScopeService scopeService) {
        this.scopeService = scopeService;
    }

    @GetMapping("/scope")
    @Operation(summary = "查询当前账号的考勤数据范围",
            description = "返回当前账号可查看的部门和员工筛选项",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<AttendanceScopeResponse> getScope(
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(scopeService.getScope(operator), traceId);
    }
}
