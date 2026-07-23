package com.personaowl.oa.attendance.application;

import com.personaowl.oa.attendance.api.dto.WorkScheduleResponse;
import com.personaowl.oa.attendance.domain.CalendarDayType;
import com.personaowl.oa.attendance.domain.RuleSnapshot;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceCalendarDayEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceCalendarDayMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceScopeMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceShiftAssignmentMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceShiftEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceShiftMapper;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkScheduleServiceTest {

    private AttendanceCalendarDayMapper calendarMapper;
    private AttendanceShiftMapper shiftMapper;
    private AttendanceRuleService ruleService;
    private WorkScheduleService service;

    @BeforeEach
    void setUp() {
        calendarMapper = mock(AttendanceCalendarDayMapper.class);
        shiftMapper = mock(AttendanceShiftMapper.class);
        ruleService = mock(AttendanceRuleService.class);
        service = new WorkScheduleService(
                calendarMapper,
                shiftMapper,
                mock(AttendanceShiftAssignmentMapper.class),
                mock(AttendanceScopeMapper.class),
                ruleService,
                mock(AttendanceAuthorizationService.class));
    }

    @Test
    void weekendDefaultsToRestDay() {
        LocalDate saturday = LocalDate.of(2026, 7, 25);

        WorkScheduleResponse result = service.resolve(1001L, saturday);

        assertFalse(result.workingDay());
        assertEquals("WEEKDAY_DEFAULT", result.source());
    }

    @Test
    void calendarOverrideCanTurnWeekendIntoWorkday() {
        LocalDate saturday = LocalDate.of(2026, 7, 25);
        AttendanceCalendarDayEntity override = new AttendanceCalendarDayEntity();
        override.setWorkDate(saturday);
        override.setDayType(CalendarDayType.WORKDAY);
        override.setHolidayName("项目调休上班");
        when(calendarMapper.selectById(saturday)).thenReturn(override);
        when(ruleService.currentSnapshot()).thenReturn(
                new RuleSnapshot(LocalTime.of(9, 0), LocalTime.of(18, 0), 5));

        WorkScheduleResponse result = service.resolve(1001L, saturday);

        assertTrue(result.workingDay());
        assertEquals(CalendarDayType.WORKDAY, result.overrideType());
        assertEquals("CALENDAR_OVERRIDE", result.source());
    }

    @Test
    void userAssignmentOverridesDefaultRule() {
        LocalDate monday = LocalDate.of(2026, 7, 27);
        AttendanceShiftEntity shift = new AttendanceShiftEntity();
        shift.setId(20L);
        shift.setName("早班");
        shift.setWorkStart(LocalTime.of(8, 0));
        shift.setWorkEnd(LocalTime.of(16, 0));
        shift.setLateThresholdMinutes(10);
        shift.setColor("#22c55e");
        when(shiftMapper.findAssignedShift(1001L, monday)).thenReturn(shift);

        WorkScheduleResponse result = service.resolve(1001L, monday);

        assertEquals("20", result.shiftId());
        assertEquals("早班", result.shiftName());
        assertEquals(LocalTime.of(8, 0), result.workStart());
        assertEquals(10, result.lateThresholdMinutes());
    }
}
