package com.personaowl.oa.attendance.application;

import com.personaowl.oa.attendance.api.dto.MonthlyStatisticsResponse;
import com.personaowl.oa.attendance.api.dto.StatisticsSummaryResponse;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRecordMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceStatisticsAggregate;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService.RecordQueryScope;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

@Service
public class AttendanceStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceStatisticsService.class);

    private final AttendanceRecordMapper recordMapper;
    private final AttendanceAuthorizationService authorizationService;
    private final Clock clock;

    public AttendanceStatisticsService(AttendanceRecordMapper recordMapper,
                                       AttendanceAuthorizationService authorizationService,
                                       Clock clock) {
        this.recordMapper = recordMapper;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MonthlyStatisticsResponse getMonthlyStatistics(OperatorContext operator, YearMonth requestedMonth) {
        long startedAt = System.nanoTime();
        YearMonth month = requestedMonth == null ? YearMonth.now(clock) : requestedMonth;
        validateMonth(month);
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        AttendanceStatisticsAggregate aggregate = aggregate(
                operator.userId(), startDate, endDate, LocalDate.now(clock), null);

        log.info("attendance.statistics.monthly traceId={} userId={} month={} totalRecords={} durationMs={}",
                operator.traceId(), operator.userId(), month, aggregate.getTotalRecords(), elapsedMillis(startedAt));
        return new MonthlyStatisticsResponse(
                String.valueOf(operator.userId()),
                month,
                startDate,
                endDate,
                aggregate.getTotalRecords(),
                aggregate.getNormalCount(),
                aggregate.getLateCount(),
                aggregate.getEarlyLeaveCount(),
                aggregate.getMissingCheckOutCount(),
                aggregate.getMissingCheckInCount(),
                aggregate.getAbsentCount());
    }

    @Transactional(readOnly = true)
    public StatisticsSummaryResponse getSummary(OperatorContext operator,
                                                LocalDate startDate,
                                                LocalDate endDate,
                                                Long requestedUserId,
                                                Long departmentId) {
        long startedAt = System.nanoTime();
        validateSummaryRange(startDate, endDate, requestedUserId, departmentId);
        RecordQueryScope scope = authorizationService.resolveStatisticsScope(
                operator, requestedUserId, departmentId);
        AttendanceStatisticsAggregate aggregate = aggregate(
                scope.targetUserId(), startDate, endDate, LocalDate.now(clock), scope.departmentIds());

        log.info("attendance.statistics.summary traceId={} operatorId={} startDate={} endDate={} "
                        + "departmentId={} totalRecords={} totalUsers={} durationMs={}",
                operator.traceId(), operator.userId(), startDate, endDate, departmentId,
                aggregate.getTotalRecords(), aggregate.getTotalUsers(), elapsedMillis(startedAt));
        return new StatisticsSummaryResponse(
                startDate,
                endDate,
                departmentId,
                scope.departmentFilterApplied(),
                scope.scopeNote(),
                aggregate.getTotalRecords(),
                aggregate.getTotalUsers(),
                aggregate.getNormalCount(),
                aggregate.getLateCount(),
                aggregate.getEarlyLeaveCount(),
                aggregate.getMissingCheckOutCount(),
                aggregate.getMissingCheckInCount(),
                aggregate.getAbsentCount());
    }

    public StatisticsSummaryResponse getSummary(OperatorContext operator,
                                                LocalDate startDate,
                                                LocalDate endDate,
                                                Long departmentId) {
        return getSummary(operator, startDate, endDate, null, departmentId);
    }

    private AttendanceStatisticsAggregate aggregate(Long userId,
                                                    LocalDate startDate,
                                                    LocalDate endDate,
                                                    LocalDate today,
                                                    java.util.List<Long> departmentIds) {
        AttendanceStatisticsAggregate aggregate = departmentIds == null || departmentIds.isEmpty()
                ? recordMapper.aggregateStatistics(userId, startDate, endDate, today)
                : recordMapper.aggregateStatistics(userId, startDate, endDate, today, departmentIds);
        return aggregate == null ? new AttendanceStatisticsAggregate() : aggregate;
    }

    private void validateMonth(YearMonth month) {
        if (month.getYear() < 1000 || month.getYear() > 9999) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "month 必须在 1000-01 到 9999-12 之间");
        }
    }

    private void validateSummaryRange(LocalDate startDate,
                                      LocalDate endDate,
                                      Long requestedUserId,
                                      Long departmentId) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "startDate 和 endDate 不能为空");
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "startDate 不能晚于 endDate");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 366) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "统计日期跨度不能超过 366 天");
        }
        if (departmentId != null && departmentId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "departmentId 必须为正整数");
        }
        if (requestedUserId != null && requestedUserId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "targetUserId 必须为正整数");
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
