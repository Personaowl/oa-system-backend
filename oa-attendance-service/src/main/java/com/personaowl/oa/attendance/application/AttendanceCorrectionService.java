package com.personaowl.oa.attendance.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaowl.oa.attendance.api.dto.AttendanceCorrectionCreateRequest;
import com.personaowl.oa.attendance.api.dto.AttendanceCorrectionResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceCorrectionReviewRequest;
import com.personaowl.oa.attendance.domain.AttendanceCorrectionStatus;
import com.personaowl.oa.attendance.domain.AttendanceCorrectionType;
import com.personaowl.oa.attendance.domain.AttendanceRuleCalculator;
import com.personaowl.oa.attendance.domain.AttendanceStatus;
import com.personaowl.oa.attendance.domain.CheckResult;
import com.personaowl.oa.attendance.domain.RuleSnapshot;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceCorrectionEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceCorrectionMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRecordEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRecordMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceScopeMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceUserDirectoryEntry;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService.RecordQueryScope;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttendanceCorrectionService {

    private final AttendanceCorrectionMapper correctionMapper;
    private final AttendanceRecordMapper recordMapper;
    private final AttendanceScopeMapper scopeMapper;
    private final AttendanceAuthorizationService authorizationService;
    private final WorkScheduleService scheduleService;
    private final AttendanceRuleCalculator ruleCalculator;
    private final Clock clock;

    public AttendanceCorrectionService(
            AttendanceCorrectionMapper correctionMapper,
            AttendanceRecordMapper recordMapper,
            AttendanceScopeMapper scopeMapper,
            AttendanceAuthorizationService authorizationService,
            WorkScheduleService scheduleService,
            AttendanceRuleCalculator ruleCalculator,
            Clock clock) {
        this.correctionMapper = correctionMapper;
        this.recordMapper = recordMapper;
        this.scopeMapper = scopeMapper;
        this.authorizationService = authorizationService;
        this.scheduleService = scheduleService;
        this.ruleCalculator = ruleCalculator;
        this.clock = clock;
    }

    @Transactional
    public AttendanceCorrectionResponse submit(
            OperatorContext operator, AttendanceCorrectionCreateRequest request) {
        LocalDate today = LocalDate.now(clock);
        if (request.workDate().isAfter(today)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不能申请未来日期的补卡");
        }
        if (!scheduleService.isWorkingDay(request.workDate())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "休息日无需申请补卡");
        }
        long duplicate = correctionMapper.selectCount(
                Wrappers.<AttendanceCorrectionEntity>lambdaQuery()
                        .eq(AttendanceCorrectionEntity::getUserId, operator.userId())
                        .eq(AttendanceCorrectionEntity::getWorkDate, request.workDate())
                        .eq(AttendanceCorrectionEntity::getCorrectionType, request.correctionType())
                        .eq(AttendanceCorrectionEntity::getStatus, AttendanceCorrectionStatus.PENDING));
        if (duplicate > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "该日期已有同类型待审批补卡申请");
        }

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        AttendanceCorrectionEntity entity = new AttendanceCorrectionEntity();
        entity.setUserId(operator.userId());
        entity.setWorkDate(request.workDate());
        entity.setCorrectionType(request.correctionType());
        entity.setCorrectionTime(LocalDateTime.of(request.workDate(), request.correctionTime()));
        entity.setReason(request.reason().trim());
        entity.setStatus(AttendanceCorrectionStatus.PENDING);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        correctionMapper.insert(entity);
        return toResponse(entity, null);
    }

    @Transactional(readOnly = true)
    public List<AttendanceCorrectionResponse> mine(OperatorContext operator) {
        List<AttendanceCorrectionEntity> rows = correctionMapper.selectList(
                Wrappers.<AttendanceCorrectionEntity>lambdaQuery()
                        .eq(AttendanceCorrectionEntity::getUserId, operator.userId())
                        .orderByDesc(AttendanceCorrectionEntity::getCreatedAt)
                        .last("LIMIT 100"));
        return toResponses(rows);
    }

    @Transactional(readOnly = true)
    public List<AttendanceCorrectionResponse> pending(OperatorContext operator) {
        RecordQueryScope scope = authorizationService.resolveRecordQueryScope(operator, null, null);
        if ("SELF".equals(scope.dataScope())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        var wrapper = Wrappers.<AttendanceCorrectionEntity>lambdaQuery()
                .eq(AttendanceCorrectionEntity::getStatus, AttendanceCorrectionStatus.PENDING);
        if (!scope.departmentIds().isEmpty()) {
            String ids = scope.departmentIds().stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
            wrapper.inSql(AttendanceCorrectionEntity::getUserId,
                    "SELECT id FROM sys_user WHERE status = 1 AND deleted = 0 "
                            + "AND department_id IN (" + ids + ")");
        }
        wrapper.orderByAsc(AttendanceCorrectionEntity::getCreatedAt).last("LIMIT 200");
        return toResponses(correctionMapper.selectList(wrapper));
    }

    @Transactional
    public AttendanceCorrectionResponse review(
            OperatorContext operator, long id, AttendanceCorrectionReviewRequest request) {
        AttendanceCorrectionEntity entity = correctionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "补卡申请不存在");
        }
        if (entity.getStatus() != AttendanceCorrectionStatus.PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "该补卡申请已经处理");
        }
        if (entity.getUserId() == operator.userId()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "不能审批自己的补卡申请");
        }
        authorizationService.resolveRecordQueryScope(operator, entity.getUserId(), null);

        boolean approved = "APPROVE".equals(request.decision());
        if (!approved && !StringUtils.hasText(request.comment())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "驳回补卡时必须填写审批意见");
        }
        if (approved) {
            applyCorrection(entity);
        }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        entity.setStatus(approved
                ? AttendanceCorrectionStatus.APPROVED
                : AttendanceCorrectionStatus.REJECTED);
        entity.setApproverId(operator.userId());
        entity.setReviewComment(StringUtils.hasText(request.comment()) ? request.comment().trim() : null);
        entity.setDecidedAt(now);
        entity.setUpdatedAt(now);
        correctionMapper.updateById(entity);
        return toResponse(entity, displayName(entity.getUserId()));
    }

    private void applyCorrection(AttendanceCorrectionEntity correction) {
        AttendanceRecordEntity record = recordMapper.selectOne(
                Wrappers.<AttendanceRecordEntity>lambdaQuery()
                        .eq(AttendanceRecordEntity::getUserId, correction.getUserId())
                        .eq(AttendanceRecordEntity::getWorkDate, correction.getWorkDate())
                        .last("LIMIT 1"));
        RuleSnapshot snapshot = record == null
                ? scheduleService.resolveRule(correction.getUserId(), correction.getWorkDate())
                : snapshotFor(record);
        if (record == null) {
            record = new AttendanceRecordEntity();
            record.setUserId(correction.getUserId());
            record.setWorkDate(correction.getWorkDate());
            record.setLateMinutes(0);
            record.setEarlyLeaveMinutes(0);
            record.setRuleWorkStart(snapshot.workStart());
            record.setRuleWorkEnd(snapshot.workEnd());
            record.setRuleLateThresholdMinutes(snapshot.lateThresholdMinutes());
        }

        if (correction.getCorrectionType() == AttendanceCorrectionType.CHECK_IN) {
            if (record.getCheckInTime() != null) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "该日期已有上班打卡");
            }
            record.setCheckInTime(correction.getCorrectionTime());
        } else {
            if (record.getCheckOutTime() != null) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "该日期已有下班打卡");
            }
            record.setCheckOutTime(correction.getCorrectionTime());
        }
        recalculate(record, snapshot);
        if (record.getId() == null) recordMapper.insert(record);
        else recordMapper.updateById(record);
    }

    private void recalculate(AttendanceRecordEntity record, RuleSnapshot snapshot) {
        if (record.getCheckInTime() == null) {
            record.setStatus(AttendanceStatus.MISSING_CHECK_IN);
            record.setLateMinutes(0);
            record.setEarlyLeaveMinutes(0);
            record.setActualWorkMinutes(0);
            return;
        }
        CheckResult checkIn = ruleCalculator.resolveCheckIn(record.getCheckInTime(), snapshot);
        record.setLateMinutes(checkIn.lateMinutes());
        if (record.getCheckOutTime() == null) {
            record.setStatus(AttendanceStatus.MISSING_CHECK_OUT);
            record.setEarlyLeaveMinutes(0);
            record.setActualWorkMinutes(0);
            return;
        }
        if (record.getCheckOutTime().isBefore(record.getCheckInTime())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "下班补卡时间不能早于上班打卡时间");
        }
        CheckResult result = ruleCalculator.resolveCheckOut(
                record.getCheckOutTime(), checkIn.late(), snapshot);
        record.setStatus(result.status());
        record.setEarlyLeaveMinutes(result.earlyLeaveMinutes());
        record.setActualWorkMinutes(Math.toIntExact(
                ChronoUnit.MINUTES.between(record.getCheckInTime(), record.getCheckOutTime())));
    }

    private RuleSnapshot snapshotFor(AttendanceRecordEntity record) {
        return record.getRuleWorkStart() != null && record.getRuleWorkEnd() != null
                && record.getRuleLateThresholdMinutes() != null
                ? new RuleSnapshot(record.getRuleWorkStart(), record.getRuleWorkEnd(),
                    record.getRuleLateThresholdMinutes())
                : scheduleService.resolveRule(record.getUserId(), record.getWorkDate());
    }

    private List<AttendanceCorrectionResponse> toResponses(List<AttendanceCorrectionEntity> rows) {
        if (rows.isEmpty()) return List.of();
        List<Long> ids = rows.stream().map(AttendanceCorrectionEntity::getUserId).distinct().toList();
        Map<Long, AttendanceUserDirectoryEntry> users = scopeMapper.findUsersByIds(ids).stream()
                .collect(Collectors.toMap(AttendanceUserDirectoryEntry::getId, Function.identity()));
        return rows.stream().map(row -> toResponse(row,
                users.get(row.getUserId()) == null ? null : users.get(row.getUserId()).getDisplayName())).toList();
    }

    private String displayName(Long userId) {
        List<AttendanceUserDirectoryEntry> users = scopeMapper.findUsersByIds(List.of(userId));
        return users.isEmpty() ? null : users.getFirst().getDisplayName();
    }

    private AttendanceCorrectionResponse toResponse(AttendanceCorrectionEntity row, String userName) {
        return new AttendanceCorrectionResponse(
                String.valueOf(row.getId()), String.valueOf(row.getUserId()), userName,
                row.getWorkDate(), row.getCorrectionType(), row.getCorrectionTime(),
                row.getReason(), row.getStatus(),
                row.getApproverId() == null ? null : String.valueOf(row.getApproverId()),
                row.getReviewComment(), row.getDecidedAt(), row.getCreatedAt());
    }
}
