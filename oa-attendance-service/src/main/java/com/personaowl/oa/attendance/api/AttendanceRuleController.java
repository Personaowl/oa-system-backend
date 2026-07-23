package com.personaowl.oa.attendance.api;

import com.personaowl.oa.attendance.api.dto.AttendanceRuleResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRuleUpdateRequest;
import com.personaowl.oa.attendance.application.AttendanceRuleService;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance/rules")
public class AttendanceRuleController {
    private final AttendanceRuleService ruleService;

    public AttendanceRuleController(AttendanceRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping("/current")
    public ApiResponse<AttendanceRuleResponse> getCurrentRule(
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(ruleService.getRule(), traceId);
    }

    @PutMapping("/current")
    public ApiResponse<AttendanceRuleResponse> updateCurrentRule(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
            @Valid @RequestBody AttendanceRuleUpdateRequest request) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(ruleService.updateRule(operator, request), traceId);
    }
}
