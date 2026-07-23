package com.personaowl.oa.attendance.api;

import com.personaowl.oa.attendance.api.dto.AttendanceFinalizationResponse;
import com.personaowl.oa.attendance.application.AttendanceFinalizationService;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceFinalizationController {

    private final AttendanceFinalizationService finalizationService;
    private final AttendanceAuthorizationService authorizationService;

    public AttendanceFinalizationController(
            AttendanceFinalizationService finalizationService,
            AttendanceAuthorizationService authorizationService) {
        this.finalizationService = finalizationService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/finalize")
    public ApiResponse<AttendanceFinalizationResponse> finalizeWorkDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        authorizationService.requireRuleUpdate(operator);
        return ApiResponse.success(finalizationService.finalizeWorkDate(workDate), traceId);
    }
}
