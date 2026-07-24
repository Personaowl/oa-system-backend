package com.personaowl.oa.attendance.application;

import com.personaowl.oa.attendance.api.dto.MonthlyStatisticsResponse;
import com.personaowl.oa.attendance.api.dto.StatisticsSummaryResponse;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRecordMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceStatisticsAggregate;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceStatisticsServiceTest {

    private static final OperatorContext EMPLOYEE = new OperatorContext(10001L, Set.of(), "trace-user");
    private static final OperatorContext ADMIN = new OperatorContext(
            90001L, Set.of(AttendanceAuthorizationService.STATISTICS_QUERY_PERMISSION,
            AttendanceAuthorizationService.ALL_DATA_SCOPE_PERMISSION), "trace-admin");

    @Mock
    private AttendanceRecordMapper recordMapper;

    private AttendanceStatisticsService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-22T01:00:00Z"), ZoneId.of("Asia/Shanghai"));
        service = new AttendanceStatisticsService(
                recordMapper, new AttendanceAuthorizationService(), clock);
    }

    @Test
    void returnsCurrentEmployeeMonthlyStatisticsByDefault() {
        AttendanceStatisticsAggregate aggregate = aggregate(5, 1, 2, 3, 2, 1);
        when(recordMapper.aggregateStatistics(
                10001L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 7, 22)))
                .thenReturn(aggregate);

        MonthlyStatisticsResponse response = service.getMonthlyStatistics(EMPLOYEE, null);

        assertEquals("10001", response.userId());
        assertEquals(YearMonth.of(2026, 7), response.month());
        assertEquals(5, response.totalRecords());
        assertEquals(2, response.normalCount());
        assertEquals(3, response.lateCount());
        assertEquals(2, response.earlyLeaveCount());
        assertEquals(1, response.missingCheckOutCount());
    }

    @Test
    void returnsAuthorizedSummaryAndExplainsReservedDepartmentFilter() {
        AttendanceStatisticsAggregate aggregate = aggregate(8, 4, 2, 3, 2, 1);
        when(recordMapper.aggregateStatistics(
                null,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 7, 22),
                java.util.List.of(10L)))
                .thenReturn(aggregate);

        StatisticsSummaryResponse response = service.getSummary(
                ADMIN, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 10L);

        assertEquals(10L, response.departmentId());
        org.junit.jupiter.api.Assertions.assertTrue(response.departmentFilterApplied());
        assertEquals("查询指定部门考勤", response.scopeNote());
        assertEquals(8, response.totalRecords());
        assertEquals(4, response.totalUsers());
    }

    @Test
    void rejectsSummaryWithoutStatisticsPermissionBeforeQueryingDatabase() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getSummary(
                        EMPLOYEE, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null));

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode());
        verifyNoInteractions(recordMapper);
    }

    @Test
    void rejectsInvalidSummaryRangesAndDepartment() {
        assertEquals(ErrorCode.INVALID_ARGUMENT,
                assertThrows(BusinessException.class,
                        () -> service.getSummary(ADMIN, null, LocalDate.of(2026, 7, 31), null)).errorCode());
        assertEquals(ErrorCode.INVALID_ARGUMENT,
                assertThrows(BusinessException.class,
                        () -> service.getSummary(
                                ADMIN, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 31), null)).errorCode());
        assertEquals(ErrorCode.INVALID_ARGUMENT,
                assertThrows(BusinessException.class,
                        () -> service.getSummary(
                                ADMIN, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 7, 31), null)).errorCode());
        assertEquals(ErrorCode.INVALID_ARGUMENT,
                assertThrows(BusinessException.class,
                        () -> service.getSummary(
                                ADMIN, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 0L)).errorCode());
        verifyNoInteractions(recordMapper);
    }

    @Test
    void returnsZeroCountsWhenMapperReturnsNoAggregateRow() {
        when(recordMapper.aggregateStatistics(
                10001L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 22)))
                .thenReturn(null);

        MonthlyStatisticsResponse response = service.getMonthlyStatistics(
                EMPLOYEE, YearMonth.of(2026, 6));

        assertEquals(0, response.totalRecords());
        assertEquals(0, response.normalCount());
        verify(recordMapper).aggregateStatistics(
                10001L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 22));
    }

    private AttendanceStatisticsAggregate aggregate(long totalRecords,
                                                    long totalUsers,
                                                    long normalCount,
                                                    long lateCount,
                                                    long earlyLeaveCount,
                                                    long missingCheckOutCount) {
        AttendanceStatisticsAggregate aggregate = new AttendanceStatisticsAggregate();
        aggregate.setTotalRecords(totalRecords);
        aggregate.setTotalUsers(totalUsers);
        aggregate.setNormalCount(normalCount);
        aggregate.setLateCount(lateCount);
        aggregate.setEarlyLeaveCount(earlyLeaveCount);
        aggregate.setMissingCheckOutCount(missingCheckOutCount);
        return aggregate;
    }
}
