package com.personaowl.oa.attendance.api;

import com.personaowl.oa.attendance.api.dto.MonthlyStatisticsResponse;
import com.personaowl.oa.attendance.api.dto.StatisticsSummaryResponse;
import com.personaowl.oa.attendance.application.AttendanceStatisticsService;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/attendance/statistics")
@Tag(name = "考勤统计", description = "个人月度统计和管理员日期范围汇总")
public class AttendanceStatisticsController {

    private final AttendanceStatisticsService statisticsService;

    public AttendanceStatisticsController(AttendanceStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/monthly")
    @Operation(summary = "查询个人月度统计", description = "按月统计当前员工的正常、迟到、早退和缺卡数量",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<MonthlyStatisticsResponse> getMonthlyStatistics(
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
            @Parameter(description = "统计月份，格式 yyyy-MM；不传时使用当前月", example = "2026-07")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(statisticsService.getMonthlyStatistics(operator, month), traceId);
    }

    @GetMapping("/summary")
    @Operation(summary = "查询管理员统计汇总",
            description = "需要 attendance:statistics:query，按日期范围汇总全部用户考勤",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<StatisticsSummaryResponse> getSummary(
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.USER_ID, required = false) String userId,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @Parameter(hidden = true) @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
            @Parameter(description = "开始日期，必填，格式 yyyy-MM-dd", example = "2026-07-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，必填，格式 yyyy-MM-dd", example = "2026-07-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "指定员工 ID")
            @RequestParam(required = false) Long targetUserId,
            @Parameter(description = "指定部门 ID")
            @RequestParam(required = false) Long departmentId) {
        OperatorContext operator = OperatorContext.fromHeaders(userId, roles, permissions, traceId);
        return ApiResponse.success(
                statisticsService.getSummary(operator, startDate, endDate, targetUserId, departmentId), traceId);
    }
}
