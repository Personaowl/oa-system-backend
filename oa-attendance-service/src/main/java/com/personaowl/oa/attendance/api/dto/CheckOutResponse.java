package com.personaowl.oa.attendance.api.dto;

import com.personaowl.oa.attendance.domain.AttendanceStatus;

import java.time.OffsetDateTime;

public record CheckOutResponse(
        String recordId,
        OffsetDateTime checkOutTime,
        AttendanceStatus status,
        boolean earlyLeave,
        int earlyLeaveMinutes
) {
}
