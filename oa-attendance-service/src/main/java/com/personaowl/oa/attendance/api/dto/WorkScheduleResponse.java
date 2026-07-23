package com.personaowl.oa.attendance.api.dto;

import com.personaowl.oa.attendance.domain.CalendarDayType;

import java.time.LocalDate;
import java.time.LocalTime;

public record WorkScheduleResponse(
        LocalDate workDate,
        boolean workingDay,
        CalendarDayType overrideType,
        String holidayName,
        String source,
        String shiftId,
        String shiftName,
        LocalTime workStart,
        LocalTime workEnd,
        int lateThresholdMinutes,
        String color
) {
}
