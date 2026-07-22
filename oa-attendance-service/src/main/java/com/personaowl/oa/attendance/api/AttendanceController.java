package com.personaowl.oa.attendance.api;

import com.personaowl.oa.attendance.api.dto.AttendanceRecordPageResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRecordQuery;
import com.personaowl.oa.attendance.api.dto.CheckInResponse;
import com.personaowl.oa.attendance.api.dto.CheckOutResponse;
import com.personaowl.oa.attendance.api.dto.TodayStatusResponse;
import com.personaowl.oa.attendance.application.AttendanceApplicationService;
import com.personaowl.oa.attendance.domain.AttendanceStatus;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendance")
@Tag(name = "考勤操作与记录", description = "当前员工打卡、今日状态和授权范围内的考勤记录查询")
public class AttendanceController {

    private final AttendanceApplicationService attendanceService;

    public AttendanceController(AttendanceApplicationService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in")
    @Operation(summary = "上班打卡", description = "使用服务器时间为当前登录员工创建当天考勤记录",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CheckInResponse> checkIn(
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, permissions, traceId);
        return ApiResponse.success(attendanceService.checkIn(operator), traceId);
    }

    @PostMapping("/check-out")
    @Operation(summary = "下班打卡", description = "完成当天考勤记录并计算最终迟到、早退状态",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<CheckOutResponse> checkOut(
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, permissions, traceId);
        return ApiResponse.success(attendanceService.checkOut(operator), traceId);
    }

    @GetMapping("/today")
    @Operation(summary = "查询今日状态", description = "返回当前员工今天的打卡时间、状态和可执行操作",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<TodayStatusResponse> getTodayStatus(
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, permissions, traceId);
        return ApiResponse.success(attendanceService.getTodayStatus(operator), traceId);
    }

    @GetMapping("/records")
    @Operation(summary = "分页查询考勤记录",
            description = "普通员工仅查询本人；跨用户或全员查询需要 attendance:record:query",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<AttendanceRecordPageResponse> getRecords(
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.USER_ID, required = false) String operatorUserId,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
            @Parameter(description = "开始日期，格式 yyyy-MM-dd", example = "2026-07-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd", example = "2026-07-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "考勤状态筛选", example = "EARLY_LEAVE")
            @RequestParam(required = false) AttendanceStatus status,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量，范围 1 到 100", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "管理员指定用户 ID；普通员工只能传本人 ID")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "预留部门参数，本期不参与数据过滤")
            @RequestParam(required = false) Long departmentId) {
        OperatorContext operator = OperatorContext.fromHeaders(operatorUserId, permissions, traceId);
        AttendanceRecordQuery query = new AttendanceRecordQuery(
                startDate, endDate, status, page, size, userId, departmentId);
        return ApiResponse.success(attendanceService.getRecords(operator, query), traceId);
    }
}
