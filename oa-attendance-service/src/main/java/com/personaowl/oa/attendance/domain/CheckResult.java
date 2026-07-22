package com.personaowl.oa.attendance.domain;

import java.util.Objects;

public record CheckResult(
        AttendanceStatus status,
        boolean late,
        int lateMinutes,
        boolean earlyLeave,
        int earlyLeaveMinutes
) {
    public CheckResult {
        Objects.requireNonNull(status, "status must not be null");
        if (lateMinutes < 0 || earlyLeaveMinutes < 0) {
            throw new IllegalArgumentException("attendance minutes must not be negative");
        }
    }
}
