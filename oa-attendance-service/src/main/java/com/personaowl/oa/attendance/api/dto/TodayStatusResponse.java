package com.personaowl.oa.attendance.api.dto;

import com.personaowl.oa.attendance.domain.AttendanceStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TodayStatusResponse(
        LocalDate workDate,
        OffsetDateTime checkInTime,
        OffsetDateTime checkOutTime,
        AttendanceStatus status,
        boolean canCheckIn,
        boolean canCheckOut
) {
}
