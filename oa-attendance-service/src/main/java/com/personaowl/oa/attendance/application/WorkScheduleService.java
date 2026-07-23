package com.personaowl.oa.attendance.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaowl.oa.attendance.api.dto.CalendarDayResponse;
import com.personaowl.oa.attendance.api.dto.CalendarDayUpdateRequest;
import com.personaowl.oa.attendance.api.dto.ShiftAssignmentRequest;
import com.personaowl.oa.attendance.api.dto.ShiftAssignmentResponse;
import com.personaowl.oa.attendance.api.dto.ShiftRequest;
import com.personaowl.oa.attendance.api.dto.ShiftResponse;
import com.personaowl.oa.attendance.api.dto.WorkScheduleResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRuleUpdateRequest;
import com.personaowl.oa.attendance.domain.CalendarDayType;
import com.personaowl.oa.attendance.domain.RuleSnapshot;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceCalendarDayEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceCalendarDayMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceScopeMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceShiftAssignmentEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceShiftAssignmentMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceShiftEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceShiftMapper;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class WorkScheduleService {

    private static final String DEFAULT_COLOR = "#409eff";

    private final AttendanceCalendarDayMapper calendarMapper;
    private final AttendanceShiftMapper shiftMapper;
    private final AttendanceShiftAssignmentMapper assignmentMapper;
    private final AttendanceScopeMapper scopeMapper;
    private final AttendanceRuleService ruleService;
    private final AttendanceAuthorizationService authorizationService;

    public WorkScheduleService(
            AttendanceCalendarDayMapper calendarMapper,
            AttendanceShiftMapper shiftMapper,
            AttendanceShiftAssignmentMapper assignmentMapper,
            AttendanceScopeMapper scopeMapper,
            AttendanceRuleService ruleService,
            AttendanceAuthorizationService authorizationService) {
        this.calendarMapper = calendarMapper;
        this.shiftMapper = shiftMapper;
        this.assignmentMapper = assignmentMapper;
        this.scopeMapper = scopeMapper;
        this.ruleService = ruleService;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public WorkScheduleResponse resolve(long userId, LocalDate workDate) {
        AttendanceCalendarDayEntity override = calendarMapper.selectById(workDate);
        boolean defaultWorkday = workDate.getDayOfWeek() != DayOfWeek.SATURDAY
                && workDate.getDayOfWeek() != DayOfWeek.SUNDAY;
        boolean workingDay = override == null
                ? defaultWorkday
                : override.getDayType() == CalendarDayType.WORKDAY;
        String source = override == null ? "WEEKDAY_DEFAULT" : "CALENDAR_OVERRIDE";
        if (!workingDay) {
            return new WorkScheduleResponse(
                    workDate, false, override == null ? null : override.getDayType(),
                    override == null ? null : override.getHolidayName(), source,
                    null, "休息日", null, null, 0, "#94a3b8");
        }

        AttendanceShiftEntity shift = shiftMapper.findAssignedShift(userId, workDate);
        if (shift == null) {
            shift = shiftMapper.selectOne(Wrappers.<AttendanceShiftEntity>lambdaQuery()
                    .eq(AttendanceShiftEntity::getIsDefault, 1)
                    .eq(AttendanceShiftEntity::getStatus, 1)
                    .orderByAsc(AttendanceShiftEntity::getId)
                    .last("LIMIT 1"));
        }
        if (shift != null) {
            return new WorkScheduleResponse(
                    workDate, true, override == null ? null : override.getDayType(),
                    override == null ? null : override.getHolidayName(),
                    source, String.valueOf(shift.getId()), shift.getName(),
                    shift.getWorkStart(), shift.getWorkEnd(),
                    shift.getLateThresholdMinutes(), shift.getColor());
        }

        RuleSnapshot fallback = ruleService.currentSnapshot();
        return new WorkScheduleResponse(
                workDate, true, override == null ? null : override.getDayType(),
                override == null ? null : override.getHolidayName(), source,
                null, "标准班次", fallback.workStart(), fallback.workEnd(),
                fallback.lateThresholdMinutes(), DEFAULT_COLOR);
    }

    public boolean isWorkingDay(LocalDate workDate) {
        AttendanceCalendarDayEntity override = calendarMapper.selectById(workDate);
        if (override != null) return override.getDayType() == CalendarDayType.WORKDAY;
        return workDate.getDayOfWeek() != DayOfWeek.SATURDAY
                && workDate.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    public RuleSnapshot resolveRule(long userId, LocalDate workDate) {
        WorkScheduleResponse schedule = resolve(userId, workDate);
        if (!schedule.workingDay()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    schedule.holidayName() == null ? "今天是休息日，无需打卡"
                            : schedule.holidayName() + "，无需打卡");
        }
        return new RuleSnapshot(
                schedule.workStart(), schedule.workEnd(), schedule.lateThresholdMinutes());
    }

    @Transactional(readOnly = true)
    public List<CalendarDayResponse> listCalendar(
            OperatorContext operator, LocalDate startDate, LocalDate endDate) {
        authorizationService.requireRuleUpdate(operator);
        validateDateRange(startDate, endDate);
        return calendarMapper.selectList(Wrappers.<AttendanceCalendarDayEntity>lambdaQuery()
                        .between(AttendanceCalendarDayEntity::getWorkDate, startDate, endDate)
                        .orderByAsc(AttendanceCalendarDayEntity::getWorkDate))
                .stream().map(this::toCalendarResponse).toList();
    }

    @Transactional
    public CalendarDayResponse updateCalendar(
            OperatorContext operator, LocalDate workDate, CalendarDayUpdateRequest request) {
        authorizationService.requireRuleUpdate(operator);
        String holidayName = StringUtils.hasText(request.holidayName())
                ? request.holidayName().trim() : null;
        if (request.dayType() == CalendarDayType.HOLIDAY && !StringUtils.hasText(holidayName)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "休息日必须填写节假日或调休名称");
        }
        AttendanceCalendarDayEntity entity = calendarMapper.selectById(workDate);
        LocalDateTime now = LocalDateTime.now();
        if (entity == null) {
            entity = new AttendanceCalendarDayEntity();
            entity.setWorkDate(workDate);
            entity.setCreatedAt(now);
        }
        entity.setDayType(request.dayType());
        entity.setHolidayName(holidayName);
        entity.setUpdatedBy(operator.userId());
        entity.setUpdatedAt(now);
        if (calendarMapper.selectById(workDate) == null) calendarMapper.insert(entity);
        else calendarMapper.updateById(entity);
        return toCalendarResponse(entity);
    }

    @Transactional
    public void deleteCalendarOverride(OperatorContext operator, LocalDate workDate) {
        authorizationService.requireRuleUpdate(operator);
        calendarMapper.deleteById(workDate);
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> listShifts() {
        return shiftMapper.selectList(Wrappers.<AttendanceShiftEntity>lambdaQuery()
                        .orderByDesc(AttendanceShiftEntity::getIsDefault)
                        .orderByAsc(AttendanceShiftEntity::getName))
                .stream().map(this::toShiftResponse).toList();
    }

    @Transactional
    public ShiftResponse createShift(OperatorContext operator, ShiftRequest request) {
        authorizationService.requireRuleUpdate(operator);
        validateShift(request);
        AttendanceShiftEntity entity = new AttendanceShiftEntity();
        applyShift(entity, request);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (request.defaultShift()) clearDefaultShift();
        shiftMapper.insert(entity);
        if (request.defaultShift()) synchronizeGlobalRule(operator, request);
        return toShiftResponse(entity);
    }

    @Transactional
    public ShiftResponse updateShift(OperatorContext operator, long id, ShiftRequest request) {
        authorizationService.requireRuleUpdate(operator);
        validateShift(request);
        AttendanceShiftEntity entity = shiftMapper.selectById(id);
        if (entity == null) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "班次不存在");
        if (request.defaultShift()) clearDefaultShift();
        applyShift(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        shiftMapper.updateById(entity);
        if (request.defaultShift()) synchronizeGlobalRule(operator, request);
        return toShiftResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ShiftAssignmentResponse> listAssignments(OperatorContext operator) {
        authorizationService.requireRuleUpdate(operator);
        return assignmentMapper.selectList(Wrappers.<AttendanceShiftAssignmentEntity>lambdaQuery()
                        .orderByDesc(AttendanceShiftAssignmentEntity::getStartDate)
                        .last("LIMIT 500"))
                .stream().map(this::toAssignmentResponse).toList();
    }

    @Transactional
    public ShiftAssignmentResponse createAssignment(
            OperatorContext operator, ShiftAssignmentRequest request) {
        authorizationService.requireRuleUpdate(operator);
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "排班结束日期不能早于开始日期");
        }
        if (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > 366) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "单次排班跨度不能超过366天");
        }
        if (scopeMapper.findUserById(request.userId()) == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "员工不存在或已停用");
        }
        AttendanceShiftEntity shift = shiftMapper.selectById(request.shiftId());
        if (shift == null || shift.getStatus() != 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "班次不存在或已停用");
        }
        if (assignmentMapper.countOverlapping(
                request.userId(), request.startDate(), request.endDate()) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "该员工在所选日期范围内已有排班");
        }
        AttendanceShiftAssignmentEntity entity = new AttendanceShiftAssignmentEntity();
        entity.setUserId(request.userId());
        entity.setShiftId(request.shiftId());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setCreatedBy(operator.userId());
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        assignmentMapper.insert(entity);
        return toAssignmentResponse(entity);
    }

    @Transactional
    public void deleteAssignment(OperatorContext operator, long id) {
        authorizationService.requireRuleUpdate(operator);
        assignmentMapper.deleteById(id);
    }

    private void clearDefaultShift() {
        AttendanceShiftEntity update = new AttendanceShiftEntity();
        update.setIsDefault(0);
        shiftMapper.update(update, Wrappers.<AttendanceShiftEntity>lambdaUpdate()
                .eq(AttendanceShiftEntity::getIsDefault, 1));
    }

    private void synchronizeGlobalRule(OperatorContext operator, ShiftRequest request) {
        ruleService.updateRule(operator, new AttendanceRuleUpdateRequest(
                request.workStart(), request.workEnd(), request.lateThresholdMinutes()));
    }

    private void validateShift(ShiftRequest request) {
        if (!request.workEnd().isAfter(request.workStart())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "下班时间必须晚于上班时间");
        }
    }

    private void applyShift(AttendanceShiftEntity entity, ShiftRequest request) {
        entity.setName(request.name().trim());
        entity.setWorkStart(request.workStart());
        entity.setWorkEnd(request.workEnd());
        entity.setLateThresholdMinutes(request.lateThresholdMinutes());
        entity.setColor(StringUtils.hasText(request.color()) ? request.color().trim() : DEFAULT_COLOR);
        entity.setIsDefault(request.defaultShift() ? 1 : 0);
        entity.setStatus(request.enabled() ? 1 : 0);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "日历日期范围无效");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 366) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "日历查询跨度不能超过366天");
        }
    }

    private CalendarDayResponse toCalendarResponse(AttendanceCalendarDayEntity entity) {
        return new CalendarDayResponse(
                entity.getWorkDate(), entity.getDayType(), entity.getHolidayName());
    }

    private ShiftResponse toShiftResponse(AttendanceShiftEntity entity) {
        return new ShiftResponse(
                String.valueOf(entity.getId()), entity.getName(),
                entity.getWorkStart(), entity.getWorkEnd(),
                entity.getLateThresholdMinutes(), entity.getColor(),
                entity.getIsDefault() == 1, entity.getStatus() == 1);
    }

    private ShiftAssignmentResponse toAssignmentResponse(AttendanceShiftAssignmentEntity entity) {
        return new ShiftAssignmentResponse(
                String.valueOf(entity.getId()), String.valueOf(entity.getUserId()),
                String.valueOf(entity.getShiftId()), entity.getStartDate(), entity.getEndDate());
    }
}
