package com.personaowl.oa.attendance.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.personaowl.oa.attendance.api.dto.AttendanceFinalizationResponse;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRecordMapper;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AttendanceFinalizationService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceFinalizationService.class);

    private final AttendanceRecordMapper recordMapper;
    private final Clock clock;
    private final WorkScheduleService scheduleService;

    public AttendanceFinalizationService(
            AttendanceRecordMapper recordMapper,
            Clock clock,
            WorkScheduleService scheduleService) {
        this.recordMapper = recordMapper;
        this.clock = clock;
        this.scheduleService = scheduleService;
    }

    /**
     * 每天凌晨检查前一天；是否需要结算由工作日历决定，
     * 因此可正确处理周末调班和法定节假日。
     */
    @Scheduled(
            cron = "${oa.attendance.finalization-cron:0 5 0 * * *}",
            zone = "${oa.attendance.zone-id:Asia/Shanghai}")
    public void finalizePreviousWorkday() {
        LocalDate workDate = LocalDate.now(clock).minusDays(1);
        if (!scheduleService.isWorkingDay(workDate)) return;
        AttendanceFinalizationResponse result = finalizeWorkDate(workDate);
        log.info("attendance.finalization.completed workDate={} missingCheckOut={} absent={}",
                result.workDate(), result.missingCheckOutCount(), result.absentCount());
    }

    @Transactional
    public AttendanceFinalizationResponse finalizeWorkDate(LocalDate workDate) {
        LocalDate today = LocalDate.now(clock);
        if (workDate == null || !workDate.isBefore(today)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "只能结算今天之前的考勤");
        }
        if (!scheduleService.isWorkingDay(workDate)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "该日期为休息日，无需执行考勤结算");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        int missingCheckOut = recordMapper.finalizeMissingCheckOut(workDate, now);
        int absent = 0;
        for (Long userId : recordMapper.findActiveAttendanceUserIds()) {
            absent += recordMapper.insertAbsentIfMissing(IdWorker.getId(), userId, workDate, now);
        }
        return new AttendanceFinalizationResponse(workDate, missingCheckOut, absent);
    }
}
