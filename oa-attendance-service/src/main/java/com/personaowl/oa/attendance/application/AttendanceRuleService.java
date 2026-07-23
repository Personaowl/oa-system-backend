package com.personaowl.oa.attendance.application;

import com.personaowl.oa.attendance.api.dto.AttendanceRuleResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRuleUpdateRequest;
import com.personaowl.oa.attendance.config.AttendanceProperties;
import com.personaowl.oa.attendance.domain.RuleSnapshot;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRuleEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRuleMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceShiftEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceShiftMapper;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AttendanceRuleService {
    private static final long ACTIVE_RULE_ID = 1L;

    private final AttendanceRuleMapper ruleMapper;
    private final AttendanceProperties properties;
    private final AttendanceAuthorizationService authorizationService;
    private final AttendanceShiftMapper shiftMapper;

    @Autowired
    public AttendanceRuleService(AttendanceRuleMapper ruleMapper,
                                 AttendanceProperties properties,
                                 AttendanceAuthorizationService authorizationService,
                                 AttendanceShiftMapper shiftMapper) {
        this.ruleMapper = ruleMapper;
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.shiftMapper = shiftMapper;
    }

    public AttendanceRuleService(AttendanceRuleMapper ruleMapper,
                                 AttendanceProperties properties,
                                 AttendanceAuthorizationService authorizationService) {
        this(ruleMapper, properties, authorizationService, null);
    }

    @Transactional(readOnly = true)
    public AttendanceRuleResponse getRule() {
        AttendanceRuleEntity entity = ruleMapper.selectById(ACTIVE_RULE_ID);
        if (entity == null) {
            RuleSnapshot fallback = properties.snapshot();
            return new AttendanceRuleResponse(
                    fallback.workStart(), fallback.workEnd(), fallback.lateThresholdMinutes(), null);
        }
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public RuleSnapshot currentSnapshot() {
        AttendanceRuleResponse rule = getRule();
        return new RuleSnapshot(rule.workStart(), rule.workEnd(), rule.lateThresholdMinutes());
    }

    @Transactional
    public AttendanceRuleResponse updateRule(OperatorContext operator, AttendanceRuleUpdateRequest request) {
        authorizationService.requireRuleUpdate(operator);
        if (request == null || request.workStart() == null || request.workEnd() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "考勤规则不能为空");
        }
        if (!request.workEnd().isAfter(request.workStart())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "下班时间必须晚于上班时间");
        }
        if (request.lateThresholdMinutes() < 0 || request.lateThresholdMinutes() > 180) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "迟到宽限时间必须在0到180分钟之间");
        }

        AttendanceRuleEntity entity = new AttendanceRuleEntity();
        entity.setId(ACTIVE_RULE_ID);
        entity.setWorkStart(request.workStart());
        entity.setWorkEnd(request.workEnd());
        entity.setLateThresholdMinutes(request.lateThresholdMinutes());
        entity.setUpdatedBy(operator.userId());
        entity.setUpdatedAt(LocalDateTime.now());

        if (ruleMapper.selectById(ACTIVE_RULE_ID) == null) {
            ruleMapper.insert(entity);
        } else {
            ruleMapper.updateById(entity);
        }
        synchronizeDefaultShift(request);
        return toResponse(entity);
    }

    private void synchronizeDefaultShift(AttendanceRuleUpdateRequest request) {
        if (shiftMapper == null) return;
        AttendanceShiftEntity defaultShift = shiftMapper.selectOne(
                Wrappers.<AttendanceShiftEntity>lambdaQuery()
                        .eq(AttendanceShiftEntity::getIsDefault, 1)
                        .orderByAsc(AttendanceShiftEntity::getId)
                        .last("LIMIT 1"));
        if (defaultShift == null) return;
        defaultShift.setWorkStart(request.workStart());
        defaultShift.setWorkEnd(request.workEnd());
        defaultShift.setLateThresholdMinutes(request.lateThresholdMinutes());
        defaultShift.setUpdatedAt(LocalDateTime.now());
        shiftMapper.updateById(defaultShift);
    }

    private AttendanceRuleResponse toResponse(AttendanceRuleEntity entity) {
        return new AttendanceRuleResponse(
                entity.getWorkStart(), entity.getWorkEnd(),
                entity.getLateThresholdMinutes(), entity.getUpdatedAt());
    }
}
