package com.personaowl.oa.attendance.api;

import com.personaowl.oa.attendance.api.dto.CalendarDayResponse;
import com.personaowl.oa.attendance.api.dto.CalendarDayUpdateRequest;
import com.personaowl.oa.attendance.api.dto.ShiftAssignmentRequest;
import com.personaowl.oa.attendance.api.dto.ShiftAssignmentResponse;
import com.personaowl.oa.attendance.api.dto.ShiftRequest;
import com.personaowl.oa.attendance.api.dto.ShiftResponse;
import com.personaowl.oa.attendance.api.dto.WorkScheduleResponse;
import com.personaowl.oa.attendance.application.WorkScheduleService;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance/schedules")
public class WorkScheduleController {

    private final WorkScheduleService scheduleService;

    public WorkScheduleController(WorkScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/today")
    public ApiResponse<WorkScheduleResponse> today(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(
                scheduleService.resolve(operator.userId(), LocalDate.now()), traceId);
    }

    @GetMapping("/calendar")
    public ApiResponse<List<CalendarDayResponse>> calendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(
                scheduleService.listCalendar(operator, startDate, endDate), traceId);
    }

    @PutMapping("/calendar/{workDate}")
    public ApiResponse<CalendarDayResponse> updateCalendar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @Valid @RequestBody CalendarDayUpdateRequest request,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(
                scheduleService.updateCalendar(operator, workDate, request), traceId);
    }

    @DeleteMapping("/calendar/{workDate}")
    public ApiResponse<Void> deleteCalendar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        scheduleService.deleteCalendarOverride(operator, workDate);
        return ApiResponse.success(null, traceId);
    }

    @GetMapping("/shifts")
    public ApiResponse<List<ShiftResponse>> shifts(
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(scheduleService.listShifts(), traceId);
    }

    @PostMapping("/shifts")
    public ApiResponse<ShiftResponse> createShift(
            @Valid @RequestBody ShiftRequest request,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(scheduleService.createShift(operator, request), traceId);
    }

    @PutMapping("/shifts/{id}")
    public ApiResponse<ShiftResponse> updateShift(
            @PathVariable long id,
            @Valid @RequestBody ShiftRequest request,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(scheduleService.updateShift(operator, id, request), traceId);
    }

    @GetMapping("/assignments")
    public ApiResponse<List<ShiftAssignmentResponse>> assignments(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(scheduleService.listAssignments(operator), traceId);
    }

    @PostMapping("/assignments")
    public ApiResponse<ShiftAssignmentResponse> createAssignment(
            @Valid @RequestBody ShiftAssignmentRequest request,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(scheduleService.createAssignment(operator, request), traceId);
    }

    @DeleteMapping("/assignments/{id}")
    public ApiResponse<Void> deleteAssignment(
            @PathVariable long id,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        scheduleService.deleteAssignment(operator, id);
        return ApiResponse.success(null, traceId);
    }
}
