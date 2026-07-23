package com.personaowl.oa.attendance.api.dto;

import java.time.LocalDate;

public record AttendanceFinalizationResponse(
        LocalDate workDate,
        int missingCheckOutCount,
        int absentCount
) {
}
