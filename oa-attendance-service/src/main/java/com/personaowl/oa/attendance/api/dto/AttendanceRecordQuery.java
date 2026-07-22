package com.personaowl.oa.attendance.api.dto;

import com.personaowl.oa.attendance.domain.AttendanceStatus;

import java.time.LocalDate;

public record AttendanceRecordQuery(
        LocalDate startDate,
        LocalDate endDate,
        AttendanceStatus status,
        int page,
        int size,
        Long userId,
        Long departmentId
) {
}
