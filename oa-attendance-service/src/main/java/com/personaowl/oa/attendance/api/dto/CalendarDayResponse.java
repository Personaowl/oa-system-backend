package com.personaowl.oa.attendance.api.dto;

import com.personaowl.oa.attendance.domain.CalendarDayType;

import java.time.LocalDate;

public record CalendarDayResponse(
        LocalDate workDate,
        CalendarDayType dayType,
        String holidayName
) {
}
