package com.personaowl.oa.attendance.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personaowl.oa.attendance.api.dto.AttendanceRecordItemResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRecordPageResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRecordQuery;
import com.personaowl.oa.attendance.api.dto.CheckInResponse;
import com.personaowl.oa.attendance.api.dto.CheckOutResponse;
import com.personaowl.oa.attendance.api.dto.TodayStatusResponse;
import com.personaowl.oa.attendance.config.AttendanceProperties;
import com.personaowl.oa.attendance.domain.AttendanceRuleCalculator;
import com.personaowl.oa.attendance.domain.AttendanceStatus;
import com.personaowl.oa.attendance.domain.CheckResult;
import com.personaowl.oa.attendance.domain.RuleSnapshot;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRecordEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRecordMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceScopeMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceUserDirectoryEntry;
import com.personaowl.oa.attendance.infrastructure.redis.AttendanceLockService;
import com.personaowl.oa.attendance.infrastructure.redis.AttendanceLockService.LockHandle;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService.RecordQueryScope;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttendanceApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceApplicationService.class);

    private final AttendanceRecordMapper recordMapper;
    private final AttendanceLockService lockService;
    private final AttendanceProperties properties;
    private final AttendanceRuleCalculator ruleCalculator;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final AttendanceAuthorizationService authorizationService;
    private final AttendanceScopeMapper scopeMapper;
    private final AttendanceRuleService ruleService;
    private final WorkScheduleService scheduleService;

    @Autowired
    public AttendanceApplicationService(AttendanceRecordMapper recordMapper,
                                        AttendanceLockService lockService,
                                        AttendanceProperties properties,
                                        AttendanceRuleCalculator ruleCalculator,
                                        Clock clock,
                                        TransactionTemplate transactionTemplate,
                                        AttendanceAuthorizationService authorizationService,
                                        AttendanceScopeMapper scopeMapper,
                                        AttendanceRuleService ruleService,
                                        WorkScheduleService scheduleService) {
        this.recordMapper = recordMapper;
        this.lockService = lockService;
        this.properties = properties;
        this.ruleCalculator = ruleCalculator;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
        this.authorizationService = authorizationService;
        this.scopeMapper = scopeMapper;
        this.ruleService = ruleService;
        this.scheduleService = scheduleService;
    }

    public AttendanceApplicationService(AttendanceRecordMapper recordMapper,
                                        AttendanceLockService lockService,
                                        AttendanceProperties properties,
                                        AttendanceRuleCalculator ruleCalculator,
                                        Clock clock,
                                        TransactionTemplate transactionTemplate,
                                        AttendanceAuthorizationService authorizationService) {
        this(recordMapper, lockService, properties, ruleCalculator, clock,
                transactionTemplate, authorizationService, null, null, null);
    }

    public CheckInResponse checkIn(OperatorContext operator) {
        long startedAt = System.nanoTime();
        LocalDateTime checkInTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        LocalDate workDate = checkInTime.toLocalDate();
        RuleSnapshot snapshot = currentRuleSnapshot(operator.userId(), workDate);
        LockHandle lock = lockService.acquire(operator.userId(), workDate);
        if (!lock.allowsProceeding()) {
            log.info("attendance.check-in.rejected traceId={} userId={} workDate={} reason=lock-contended",
                    operator.traceId(), operator.userId(), workDate);
            throw new BusinessException(ErrorCode.ATTENDANCE_REQUEST_IN_PROGRESS);
        }

        try {
            CheckInResponse response = transactionTemplate.execute(status ->
                    doCheckIn(operator, workDate, checkInTime, snapshot));
            response = Objects.requireNonNull(response, "check-in transaction returned no result");
            log.info("attendance.check-in.success traceId={} userId={} workDate={} status={} lateMinutes={}",
                    operator.traceId(), operator.userId(), workDate, response.status(), response.lateMinutes());
            return response;
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.ATTENDANCE_ALREADY_CHECKED_IN);
        } finally {
            lockService.release(lock);
            log.debug("attendance.check-in.finished traceId={} userId={} workDate={} durationMs={}",
                    operator.traceId(), operator.userId(), workDate, elapsedMillis(startedAt));
        }
    }

    public CheckOutResponse checkOut(OperatorContext operator) {
        long startedAt = System.nanoTime();
        LocalDateTime checkOutTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        LocalDate workDate = checkOutTime.toLocalDate();
        LockHandle lock = lockService.acquire(operator.userId(), workDate);
        if (!lock.allowsProceeding()) {
            log.info("attendance.check-out.rejected traceId={} userId={} workDate={} reason=lock-contended",
                    operator.traceId(), operator.userId(), workDate);
            throw new BusinessException(ErrorCode.ATTENDANCE_REQUEST_IN_PROGRESS);
        }

        try {
            CheckOutResponse response = transactionTemplate.execute(status ->
                    doCheckOut(operator, workDate, checkOutTime));
            response = Objects.requireNonNull(response, "check-out transaction returned no result");
            log.info("attendance.check-out.success traceId={} userId={} workDate={} status={} earlyLeaveMinutes={}",
                    operator.traceId(), operator.userId(), workDate,
                    response.status(), response.earlyLeaveMinutes());
            return response;
        } catch (BusinessException exception) {
            log.info("attendance.check-out.rejected traceId={} userId={} workDate={} reason={}",
                    operator.traceId(), operator.userId(), workDate, exception.errorCode().code());
            throw exception;
        } finally {
            lockService.release(lock);
            log.debug("attendance.check-out.finished traceId={} userId={} workDate={} durationMs={}",
                    operator.traceId(), operator.userId(), workDate, elapsedMillis(startedAt));
        }
    }

    @Transactional(readOnly = true)
    public TodayStatusResponse getTodayStatus(OperatorContext operator) {
        LocalDate today = LocalDate.now(clock);
        AttendanceRecordEntity todayRecord = recordMapper.selectOne(
                Wrappers.<AttendanceRecordEntity>lambdaQuery()
                        .eq(AttendanceRecordEntity::getUserId, operator.userId())
                        .eq(AttendanceRecordEntity::getWorkDate, today)
                        .last("LIMIT 1"));

        if (todayRecord != null) {
            return toTodayStatus(todayRecord, false);
        }

        AttendanceRecordEntity historicalOpenRecord = recordMapper.selectOne(
                Wrappers.<AttendanceRecordEntity>lambdaQuery()
                        .eq(AttendanceRecordEntity::getUserId, operator.userId())
                        .lt(AttendanceRecordEntity::getWorkDate, today)
                        .isNotNull(AttendanceRecordEntity::getCheckInTime)
                        .isNull(AttendanceRecordEntity::getCheckOutTime)
                        .orderByDesc(AttendanceRecordEntity::getWorkDate)
                        .last("LIMIT 1"));
        if (historicalOpenRecord != null) {
            return toTodayStatus(historicalOpenRecord, true);
        }

        return new TodayStatusResponse(today, null, null, null, true, false);
    }

    @Transactional(readOnly = true)
    public AttendanceRecordPageResponse getRecords(OperatorContext operator, AttendanceRecordQuery query) {
        long startedAt = System.nanoTime();
        validateRecordQuery(query);
        RecordQueryScope scope = authorizationService.resolveRecordQueryScope(
                operator, query.userId(), query.departmentId());

        LocalDate today = LocalDate.now(clock);
        LambdaQueryWrapper<AttendanceRecordEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(scope.targetUserId() != null, AttendanceRecordEntity::getUserId, scope.targetUserId())
                .ge(query.startDate() != null, AttendanceRecordEntity::getWorkDate, query.startDate())
                .le(query.endDate() != null, AttendanceRecordEntity::getWorkDate, query.endDate());
        if (!scope.departmentIds().isEmpty()) {
            wrapper.inSql(AttendanceRecordEntity::getUserId,
                    "SELECT id FROM sys_user WHERE status = 1 AND deleted = 0 AND department_id IN ("
                            + scope.departmentIds().stream().map(String::valueOf).collect(Collectors.joining(","))
                            + ")");
        }
        applyStatusFilter(wrapper, query.status(), today);
        wrapper.orderByDesc(AttendanceRecordEntity::getWorkDate)
                .orderByDesc(AttendanceRecordEntity::getId);

        IPage<AttendanceRecordEntity> result = recordMapper.selectPage(
                new Page<>(query.page(), query.size()), wrapper);
        Map<Long, AttendanceUserDirectoryEntry> users = loadUsers(result.getRecords());
        List<AttendanceRecordItemResponse> items = result.getRecords().stream()
                .map(record -> toRecordItem(record, today, users.get(record.getUserId())))
                .toList();

        log.info("attendance.query.{} traceId={} operatorId={} targetUserId={} departmentId={} "
                        + "startDate={} endDate={} status={} page={} size={} resultCount={} durationMs={}",
                "SELF".equals(scope.dataScope()) ? "self" : "admin",
                operator.traceId(), operator.userId(), scope.targetUserId(), query.departmentId(),
                query.startDate(), query.endDate(), query.status(), query.page(), query.size(),
                items.size(), elapsedMillis(startedAt));
        return new AttendanceRecordPageResponse(
                items,
                query.page(),
                query.size(),
                result.getTotal(),
                scope.dataScope(),
                scope.departmentFilterApplied(),
                scope.scopeNote());
    }

    private CheckInResponse doCheckIn(OperatorContext operator,
                                      LocalDate workDate,
                                      LocalDateTime checkInTime,
                                      RuleSnapshot snapshot) {
        AttendanceRecordEntity existing = recordMapper.selectOne(
                Wrappers.<AttendanceRecordEntity>lambdaQuery()
                        .eq(AttendanceRecordEntity::getUserId, operator.userId())
                        .eq(AttendanceRecordEntity::getWorkDate, workDate)
                        .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.ATTENDANCE_ALREADY_CHECKED_IN);
        }

        CheckResult result = ruleCalculator.resolveCheckIn(checkInTime, snapshot);
        AttendanceRecordEntity record = new AttendanceRecordEntity();
        record.setUserId(operator.userId());
        record.setWorkDate(workDate);
        record.setCheckInTime(checkInTime);
        record.setStatus(result.status());
        record.setLateMinutes(result.lateMinutes());
        record.setEarlyLeaveMinutes(0);
        record.setRuleWorkStart(snapshot.workStart());
        record.setRuleWorkEnd(snapshot.workEnd());
        record.setRuleLateThresholdMinutes(snapshot.lateThresholdMinutes());

        if (recordMapper.insert(record) != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        if (record.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "考勤记录主键生成失败");
        }

        return new CheckInResponse(
                String.valueOf(record.getId()),
                workDate,
                checkInTime.atZone(clock.getZone()).toOffsetDateTime(),
                result.status(),
                result.late(),
                result.lateMinutes());
    }

    private CheckOutResponse doCheckOut(OperatorContext operator,
                                        LocalDate workDate,
                                        LocalDateTime checkOutTime) {
        AttendanceRecordEntity record = recordMapper.selectOne(
                Wrappers.<AttendanceRecordEntity>lambdaQuery()
                        .eq(AttendanceRecordEntity::getUserId, operator.userId())
                        .eq(AttendanceRecordEntity::getWorkDate, workDate)
                        .last("LIMIT 1"));
        if (record == null || record.getCheckInTime() == null) {
            throw new BusinessException(ErrorCode.ATTENDANCE_CHECK_IN_REQUIRED);
        }
        if (record.getCheckOutTime() != null) {
            throw new BusinessException(ErrorCode.ATTENDANCE_ALREADY_CHECKED_OUT);
        }
        if (record.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "考勤记录主键缺失");
        }

        boolean wasLate = record.getLateMinutes() > 0
                || (record.getStatus() != null && record.getStatus().indicatesLate());
        CheckResult result = ruleCalculator.resolveCheckOut(
                checkOutTime, wasLate, ruleSnapshotFor(record));
        int version = record.getVersion() == null ? 0 : record.getVersion();
        int updated = recordMapper.completeCheckOut(
                record.getId(),
                checkOutTime,
                result.status(),
                result.earlyLeaveMinutes(),
                version);
        if (updated != 1) {
            AttendanceRecordEntity latest = recordMapper.selectById(record.getId());
            if (latest == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "考勤记录不存在");
            }
            throw new BusinessException(ErrorCode.ATTENDANCE_ALREADY_CHECKED_OUT);
        }

        return new CheckOutResponse(
                String.valueOf(record.getId()),
                atOffset(checkOutTime),
                result.status(),
                result.earlyLeave(),
                result.earlyLeaveMinutes());
    }

    private RuleSnapshot ruleSnapshotFor(AttendanceRecordEntity record) {
        if (record.getRuleWorkStart() != null
                && record.getRuleWorkEnd() != null
                && record.getRuleLateThresholdMinutes() != null) {
            return new RuleSnapshot(
                    record.getRuleWorkStart(),
                    record.getRuleWorkEnd(),
                    record.getRuleLateThresholdMinutes());
        }
        log.warn("attendance.rule-snapshot.missing recordId={} fallbackUsed=true", record.getId());
        return currentRuleSnapshot(record.getUserId(), record.getWorkDate());
    }

    private RuleSnapshot currentRuleSnapshot(long userId, LocalDate workDate) {
        if (scheduleService != null) return scheduleService.resolveRule(userId, workDate);
        return ruleService == null ? properties.snapshot() : ruleService.currentSnapshot();
    }

    private void validateRecordQuery(AttendanceRecordQuery query) {
        if (query.page() < 1 || query.size() < 1 || query.size() > 100) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1，size 必须在 1 到 100 之间");
        }
        if (query.userId() != null && query.userId() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "userId 必须为正整数");
        }
        if (query.departmentId() != null && query.departmentId() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "departmentId 必须为正整数");
        }
        if (query.startDate() != null && query.endDate() != null) {
            if (query.startDate().isAfter(query.endDate())) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "startDate 不能晚于 endDate");
            }
            if (ChronoUnit.DAYS.between(query.startDate(), query.endDate()) > 366) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "查询日期跨度不能超过 366 天");
            }
        }
    }

    private void applyStatusFilter(LambdaQueryWrapper<AttendanceRecordEntity> wrapper,
                                   AttendanceStatus status,
                                   LocalDate today) {
        if (status == null) {
            return;
        }
        if (status == AttendanceStatus.MISSING_CHECK_OUT) {
            wrapper.lt(AttendanceRecordEntity::getWorkDate, today)
                    .isNotNull(AttendanceRecordEntity::getCheckInTime)
                    .isNull(AttendanceRecordEntity::getCheckOutTime);
            return;
        }
        wrapper.eq(AttendanceRecordEntity::getStatus, status);
        if (status == AttendanceStatus.IN_PROGRESS || status == AttendanceStatus.IN_PROGRESS_LATE) {
            wrapper.ge(AttendanceRecordEntity::getWorkDate, today);
        }
    }

    private Map<Long, AttendanceUserDirectoryEntry> loadUsers(List<AttendanceRecordEntity> records) {
        if (scopeMapper == null || records.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = records.stream().map(AttendanceRecordEntity::getUserId).distinct().toList();
        return scopeMapper.findUsersByIds(ids).stream().collect(Collectors.toMap(
                AttendanceUserDirectoryEntry::getId, Function.identity(), (left, right) -> left));
    }

    private AttendanceRecordItemResponse toRecordItem(AttendanceRecordEntity record,
                                                       LocalDate today,
                                                       AttendanceUserDirectoryEntry user) {
        AttendanceStatus status = record.getStatus();
        if (record.getWorkDate() != null
                && record.getWorkDate().isBefore(today)
                && record.getCheckInTime() != null
                && record.getCheckOutTime() == null) {
            status = AttendanceStatus.MISSING_CHECK_OUT;
        }
        return new AttendanceRecordItemResponse(
                String.valueOf(record.getId()),
                String.valueOf(record.getUserId()),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getDisplayName(),
                user == null || user.getDepartmentId() == null ? null : String.valueOf(user.getDepartmentId()),
                user == null ? null : user.getDepartmentName(),
                record.getWorkDate(),
                atOffset(record.getCheckInTime()),
                atOffset(record.getCheckOutTime()),
                status,
                record.getLateMinutes(),
                record.getEarlyLeaveMinutes());
    }

    private TodayStatusResponse toTodayStatus(AttendanceRecordEntity record, boolean historicalOpenRecord) {
        if (historicalOpenRecord) {
            return new TodayStatusResponse(
                    record.getWorkDate(),
                    atOffset(record.getCheckInTime()),
                    null,
                    AttendanceStatus.MISSING_CHECK_OUT,
                    true,
                    false);
        }

        boolean canCheckIn = record.getCheckInTime() == null
                && record.getStatus() != AttendanceStatus.LEAVE;
        boolean canCheckOut = record.getCheckInTime() != null
                && record.getCheckOutTime() == null
                && (record.getStatus() == AttendanceStatus.IN_PROGRESS
                || record.getStatus() == AttendanceStatus.IN_PROGRESS_LATE);
        return new TodayStatusResponse(
                record.getWorkDate(),
                atOffset(record.getCheckInTime()),
                atOffset(record.getCheckOutTime()),
                record.getStatus(),
                canCheckIn,
                canCheckOut);
    }

    private OffsetDateTime atOffset(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(clock.getZone()).toOffsetDateTime();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
