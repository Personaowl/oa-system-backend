package com.personaowl.oa.attendance.application;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personaowl.oa.attendance.api.dto.AttendanceRecordPageResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRecordQuery;
import com.personaowl.oa.attendance.api.dto.CheckInResponse;
import com.personaowl.oa.attendance.api.dto.CheckOutResponse;
import com.personaowl.oa.attendance.api.dto.TodayStatusResponse;
import com.personaowl.oa.attendance.config.AttendanceProperties;
import com.personaowl.oa.attendance.domain.AttendanceRuleCalculator;
import com.personaowl.oa.attendance.domain.AttendanceStatus;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRecordEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRecordMapper;
import com.personaowl.oa.attendance.infrastructure.redis.AttendanceLockService;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceApplicationServiceTest {

    private static final long USER_ID = 10001L;
    private static final OperatorContext OPERATOR = new OperatorContext(USER_ID, Set.of(), "trace-1");

    @Mock
    private AttendanceRecordMapper recordMapper;

    @Mock
    private AttendanceLockService lockService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TransactionStatus transactionStatus;

    private AttendanceApplicationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-21T01:03:12Z"), ZoneId.of("Asia/Shanghai"));
        service = new AttendanceApplicationService(
                recordMapper,
                lockService,
                new AttendanceProperties(),
                new AttendanceRuleCalculator(),
                clock,
                transactionTemplate,
                new AttendanceAuthorizationService());
    }

    @Test
    void checksInAndStoresRuleSnapshotInsideTransaction() {
        stubTransactionExecution();
        AttendanceLockService.LockHandle lock = AttendanceLockService.LockHandle.acquired("key", "token");
        when(lockService.acquire(USER_ID, java.time.LocalDate.of(2026, 7, 21))).thenReturn(lock);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(recordMapper.insert(any(AttendanceRecordEntity.class))).thenAnswer(invocation -> {
            AttendanceRecordEntity record = invocation.getArgument(0);
            record.setId(1946539000000000001L);
            return 1;
        });

        CheckInResponse response = service.checkIn(OPERATOR);

        assertEquals("1946539000000000001", response.recordId());
        assertEquals(AttendanceStatus.IN_PROGRESS, response.status());
        assertFalse(response.late());
        ArgumentCaptor<AttendanceRecordEntity> captor = ArgumentCaptor.forClass(AttendanceRecordEntity.class);
        verify(recordMapper).insert(captor.capture());
        assertEquals(java.time.LocalTime.of(9, 0), captor.getValue().getRuleWorkStart());
        assertEquals(java.time.LocalTime.of(18, 0), captor.getValue().getRuleWorkEnd());
        assertEquals(5, captor.getValue().getRuleLateThresholdMinutes());
        verify(lockService).release(lock);
    }

    @Test
    void rejectsExistingDailyRecord() {
        stubTransactionExecution();
        AttendanceLockService.LockHandle lock = AttendanceLockService.LockHandle.acquired("key", "token");
        when(lockService.acquire(any(Long.class), any(java.time.LocalDate.class))).thenReturn(lock);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(new AttendanceRecordEntity());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.checkIn(OPERATOR));

        assertEquals(ErrorCode.ATTENDANCE_ALREADY_CHECKED_IN, exception.errorCode());
        verify(recordMapper, never()).insert(any(AttendanceRecordEntity.class));
        verify(lockService).release(lock);
    }

    @Test
    void mapsDatabaseUniqueViolationToDuplicateCheckIn() {
        stubTransactionExecution();
        AttendanceLockService.LockHandle lock = AttendanceLockService.LockHandle.bypass();
        when(lockService.acquire(any(Long.class), any(java.time.LocalDate.class))).thenReturn(lock);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(recordMapper.insert(any(AttendanceRecordEntity.class)))
                .thenThrow(new DuplicateKeyException("uk_attendance_user_date"));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.checkIn(OPERATOR));

        assertEquals(ErrorCode.ATTENDANCE_ALREADY_CHECKED_IN, exception.errorCode());
        verify(lockService).release(lock);
    }

    @Test
    void rejectsLockContentionBeforeOpeningTransaction() {
        when(lockService.acquire(any(Long.class), any(java.time.LocalDate.class)))
                .thenReturn(AttendanceLockService.LockHandle.contended());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.checkIn(OPERATOR));

        assertEquals(ErrorCode.ATTENDANCE_REQUEST_IN_PROGRESS, exception.errorCode());
        verifyNoInteractions(recordMapper);
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void returnsCheckInAvailabilityWhenNoAttendanceRecordExists() {
        when(recordMapper.selectOne(any(Wrapper.class)))
                .thenReturn((AttendanceRecordEntity) null)
                .thenReturn(null);

        TodayStatusResponse response = service.getTodayStatus(OPERATOR);

        assertEquals(LocalDate.of(2026, 7, 21), response.workDate());
        assertNull(response.checkInTime());
        assertNull(response.checkOutTime());
        assertNull(response.status());
        assertTrue(response.canCheckIn());
        assertFalse(response.canCheckOut());
    }

    @Test
    void returnsCurrentOpenRecordWithCheckOutAvailability() {
        AttendanceRecordEntity record = attendanceRecord(
                LocalDate.of(2026, 7, 21),
                LocalDateTime.of(2026, 7, 21, 9, 3),
                null,
                AttendanceStatus.IN_PROGRESS);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(record);

        TodayStatusResponse response = service.getTodayStatus(OPERATOR);

        assertEquals("2026-07-21T09:03+08:00", response.checkInTime().toString());
        assertEquals(AttendanceStatus.IN_PROGRESS, response.status());
        assertFalse(response.canCheckIn());
        assertTrue(response.canCheckOut());
    }

    @Test
    void returnsCompletedCurrentRecordWithNoAvailableAction() {
        AttendanceRecordEntity record = attendanceRecord(
                LocalDate.of(2026, 7, 21),
                LocalDateTime.of(2026, 7, 21, 9, 3),
                LocalDateTime.of(2026, 7, 21, 18, 5),
                AttendanceStatus.NORMAL);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(record);

        TodayStatusResponse response = service.getTodayStatus(OPERATOR);

        assertEquals("2026-07-21T18:05+08:00", response.checkOutTime().toString());
        assertFalse(response.canCheckIn());
        assertFalse(response.canCheckOut());
    }

    @Test
    void derivesMissingCheckOutFromLatestHistoricalOpenRecord() {
        AttendanceRecordEntity historicalRecord = attendanceRecord(
                LocalDate.of(2026, 7, 20),
                LocalDateTime.of(2026, 7, 20, 9, 8),
                null,
                AttendanceStatus.IN_PROGRESS_LATE);
        when(recordMapper.selectOne(any(Wrapper.class)))
                .thenReturn((AttendanceRecordEntity) null)
                .thenReturn(historicalRecord);

        TodayStatusResponse response = service.getTodayStatus(OPERATOR);

        assertEquals(LocalDate.of(2026, 7, 20), response.workDate());
        assertEquals(AttendanceStatus.MISSING_CHECK_OUT, response.status());
        assertTrue(response.canCheckIn());
        assertFalse(response.canCheckOut());
    }

    @Test
    void checksOutNormallyAtExactWorkEnd() {
        service = serviceAt("2026-07-21T10:00:00Z", new AttendanceProperties());
        stubTransactionExecution();
        AttendanceLockService.LockHandle lock = AttendanceLockService.LockHandle.acquired("key", "token");
        AttendanceRecordEntity record = attendanceRecord(
                LocalDate.of(2026, 7, 21),
                LocalDateTime.of(2026, 7, 21, 9, 3),
                null,
                AttendanceStatus.IN_PROGRESS);
        when(lockService.acquire(USER_ID, LocalDate.of(2026, 7, 21))).thenReturn(lock);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(record);
        when(recordMapper.completeCheckOut(
                record.getId(),
                LocalDateTime.of(2026, 7, 21, 18, 0),
                AttendanceStatus.NORMAL,
                0,
                0)).thenReturn(1);

        CheckOutResponse response = service.checkOut(OPERATOR);

        assertEquals(String.valueOf(record.getId()), response.recordId());
        assertEquals("2026-07-21T18:00+08:00", response.checkOutTime().toString());
        assertEquals(AttendanceStatus.NORMAL, response.status());
        assertFalse(response.earlyLeave());
        assertEquals(0, response.earlyLeaveMinutes());
        verify(lockService).release(lock);
    }

    @Test
    void usesFrozenRuleSnapshotForLateAndEarlyLeaveStatus() {
        AttendanceProperties currentProperties = new AttendanceProperties();
        currentProperties.setWorkEnd(LocalTime.of(17, 0));
        service = serviceAt("2026-07-21T09:30:00Z", currentProperties);
        stubTransactionExecution();
        AttendanceLockService.LockHandle lock = AttendanceLockService.LockHandle.acquired("key", "token");
        AttendanceRecordEntity record = attendanceRecord(
                LocalDate.of(2026, 7, 21),
                LocalDateTime.of(2026, 7, 21, 9, 10),
                null,
                AttendanceStatus.IN_PROGRESS_LATE);
        when(lockService.acquire(any(Long.class), any(LocalDate.class))).thenReturn(lock);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(record);
        when(recordMapper.completeCheckOut(
                record.getId(),
                LocalDateTime.of(2026, 7, 21, 17, 30),
                AttendanceStatus.LATE_AND_EARLY_LEAVE,
                30,
                0)).thenReturn(1);

        CheckOutResponse response = service.checkOut(OPERATOR);

        assertEquals(AttendanceStatus.LATE_AND_EARLY_LEAVE, response.status());
        assertTrue(response.earlyLeave());
        assertEquals(30, response.earlyLeaveMinutes());
    }

    @Test
    void rejectsCheckOutWithoutCheckIn() {
        stubTransactionExecution();
        AttendanceLockService.LockHandle lock = AttendanceLockService.LockHandle.acquired("key", "token");
        when(lockService.acquire(any(Long.class), any(LocalDate.class))).thenReturn(lock);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.checkOut(OPERATOR));

        assertEquals(ErrorCode.ATTENDANCE_CHECK_IN_REQUIRED, exception.errorCode());
        verify(recordMapper, never()).completeCheckOut(anyLong(), any(), any(), anyInt(), anyInt());
        verify(lockService).release(lock);
    }

    @Test
    void rejectsAlreadyCompletedCheckOutWithoutOverwritingTime() {
        stubTransactionExecution();
        AttendanceLockService.LockHandle lock = AttendanceLockService.LockHandle.acquired("key", "token");
        AttendanceRecordEntity record = attendanceRecord(
                LocalDate.of(2026, 7, 21),
                LocalDateTime.of(2026, 7, 21, 9, 3),
                LocalDateTime.of(2026, 7, 21, 18, 5),
                AttendanceStatus.NORMAL);
        when(lockService.acquire(any(Long.class), any(LocalDate.class))).thenReturn(lock);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.checkOut(OPERATOR));

        assertEquals(ErrorCode.ATTENDANCE_ALREADY_CHECKED_OUT, exception.errorCode());
        verify(recordMapper, never()).completeCheckOut(anyLong(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void mapsConditionalUpdateConflictToAlreadyCheckedOut() {
        stubTransactionExecution();
        AttendanceLockService.LockHandle lock = AttendanceLockService.LockHandle.bypass();
        AttendanceRecordEntity record = attendanceRecord(
                LocalDate.of(2026, 7, 21),
                LocalDateTime.of(2026, 7, 21, 9, 3),
                null,
                AttendanceStatus.IN_PROGRESS);
        AttendanceRecordEntity latest = attendanceRecord(
                LocalDate.of(2026, 7, 21),
                LocalDateTime.of(2026, 7, 21, 9, 3),
                LocalDateTime.of(2026, 7, 21, 18, 1),
                AttendanceStatus.NORMAL);
        when(lockService.acquire(any(Long.class), any(LocalDate.class))).thenReturn(lock);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(record);
        when(recordMapper.completeCheckOut(anyLong(), any(), any(), anyInt(), anyInt()))
                .thenReturn(0);
        when(recordMapper.selectById(record.getId())).thenReturn(latest);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.checkOut(OPERATOR));

        assertEquals(ErrorCode.ATTENDANCE_ALREADY_CHECKED_OUT, exception.errorCode());
        verify(recordMapper).selectById(record.getId());
    }

    @Test
    void rejectsCheckOutLockContentionBeforeOpeningTransaction() {
        when(lockService.acquire(any(Long.class), any(LocalDate.class)))
                .thenReturn(AttendanceLockService.LockHandle.contended());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.checkOut(OPERATOR));

        assertEquals(ErrorCode.ATTENDANCE_REQUEST_IN_PROGRESS, exception.errorCode());
        verifyNoInteractions(recordMapper);
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void returnsPagedSelfRecordsAndDerivesHistoricalMissingCheckOut() {
        AttendanceRecordEntity record = attendanceRecord(
                LocalDate.of(2026, 7, 20),
                LocalDateTime.of(2026, 7, 20, 9, 8),
                null,
                AttendanceStatus.IN_PROGRESS_LATE);
        when(recordMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<AttendanceRecordEntity> page = invocation.getArgument(0);
            page.setRecords(List.of(record));
            page.setTotal(1);
            return page;
        });

        AttendanceRecordPageResponse response = service.getRecords(
                OPERATOR,
                new AttendanceRecordQuery(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 21),
                        null,
                        1,
                        20,
                        null,
                        null));

        assertEquals(1, response.items().size());
        assertEquals("1946539000000000001", response.items().getFirst().recordId());
        assertEquals("10001", response.items().getFirst().userId());
        assertEquals(AttendanceStatus.MISSING_CHECK_OUT, response.items().getFirst().status());
        assertEquals(1, response.total());
        assertEquals("SELF", response.dataScope());
        assertFalse(response.departmentFilterApplied());
    }

    @Test
    void rejectsInvalidRecordQueryParametersBeforeDatabaseAccess() {
        AttendanceRecordQuery reversedDates = new AttendanceRecordQuery(
                LocalDate.of(2026, 7, 21), LocalDate.of(2026, 7, 1), null, 1, 20, null, null);
        AttendanceRecordQuery excessiveRange = new AttendanceRecordQuery(
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 7, 21), null, 1, 20, null, null);
        AttendanceRecordQuery invalidPage = new AttendanceRecordQuery(null, null, null, 0, 20, null, null);
        AttendanceRecordQuery excessiveSize = new AttendanceRecordQuery(null, null, null, 1, 101, null, null);
        AttendanceRecordQuery invalidUser = new AttendanceRecordQuery(null, null, null, 1, 20, 0L, null);

        assertEquals(ErrorCode.INVALID_ARGUMENT,
                assertThrows(BusinessException.class, () -> service.getRecords(OPERATOR, reversedDates)).errorCode());
        assertEquals(ErrorCode.INVALID_ARGUMENT,
                assertThrows(BusinessException.class, () -> service.getRecords(OPERATOR, excessiveRange)).errorCode());
        assertEquals(ErrorCode.INVALID_ARGUMENT,
                assertThrows(BusinessException.class, () -> service.getRecords(OPERATOR, invalidPage)).errorCode());
        assertEquals(ErrorCode.INVALID_ARGUMENT,
                assertThrows(BusinessException.class, () -> service.getRecords(OPERATOR, excessiveSize)).errorCode());
        assertEquals(ErrorCode.INVALID_ARGUMENT,
                assertThrows(BusinessException.class, () -> service.getRecords(OPERATOR, invalidUser)).errorCode());
        verifyNoInteractions(recordMapper);
    }

    private AttendanceRecordEntity attendanceRecord(LocalDate workDate,
                                                      LocalDateTime checkInTime,
                                                      LocalDateTime checkOutTime,
                                                      AttendanceStatus status) {
        AttendanceRecordEntity record = new AttendanceRecordEntity();
        record.setUserId(USER_ID);
        record.setWorkDate(workDate);
        record.setCheckInTime(checkInTime);
        record.setCheckOutTime(checkOutTime);
        record.setStatus(status);
        record.setId(1946539000000000001L);
        record.setLateMinutes(status != null && status.indicatesLate() ? 5 : 0);
        record.setRuleWorkStart(LocalTime.of(9, 0));
        record.setRuleWorkEnd(LocalTime.of(18, 0));
        record.setRuleLateThresholdMinutes(5);
        record.setVersion(0);
        return record;
    }

    private AttendanceApplicationService serviceAt(String instant, AttendanceProperties currentProperties) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Shanghai"));
        return new AttendanceApplicationService(
                recordMapper,
                lockService,
                currentProperties,
                new AttendanceRuleCalculator(),
                clock,
                transactionTemplate,
                new AttendanceAuthorizationService());
    }

    private void stubTransactionExecution() {
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        }).when(transactionTemplate).execute(any());
    }
}
