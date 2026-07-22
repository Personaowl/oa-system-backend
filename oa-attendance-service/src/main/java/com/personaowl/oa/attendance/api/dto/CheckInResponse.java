package com.personaowl.oa.attendance.api.dto;

import com.personaowl.oa.attendance.domain.AttendanceStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CheckInResponse(
        String recordId,
        LocalDate workDate,
        OffsetDateTime checkInTime,
        AttendanceStatus status,
        boolean late,
        int lateMinutes
) {
}
