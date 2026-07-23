package com.personaowl.oa.attendance.api;

import com.personaowl.oa.attendance.api.dto.AttendanceCorrectionCreateRequest;
import com.personaowl.oa.attendance.api.dto.AttendanceCorrectionResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceCorrectionReviewRequest;
import com.personaowl.oa.attendance.application.AttendanceCorrectionService;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance/corrections")
public class AttendanceCorrectionController {

    private final AttendanceCorrectionService correctionService;

    public AttendanceCorrectionController(AttendanceCorrectionService correctionService) {
        this.correctionService = correctionService;
    }

    @PostMapping
    public ApiResponse<AttendanceCorrectionResponse> submit(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
            @Valid @RequestBody AttendanceCorrectionCreateRequest request) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(correctionService.submit(operator, request), traceId);
    }

    @GetMapping("/mine")
    public ApiResponse<List<AttendanceCorrectionResponse>> mine(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(correctionService.mine(operator), traceId);
    }

    @GetMapping("/pending")
    public ApiResponse<List<AttendanceCorrectionResponse>> pending(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(correctionService.pending(operator), traceId);
    }

    @PostMapping("/{id}/review")
    public ApiResponse<AttendanceCorrectionResponse> review(
            @PathVariable long id,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
            @Valid @RequestBody AttendanceCorrectionReviewRequest request) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(correctionService.review(operator, id, request), traceId);
    }
}
