package com.personaowl.oa.attendance.domain;

public enum AttendanceStatus {
    IN_PROGRESS,
    IN_PROGRESS_LATE,
    NORMAL,
    LATE,
    EARLY_LEAVE,
    LATE_AND_EARLY_LEAVE,
    MISSING_CHECK_OUT,
    LEAVE;

    public boolean indicatesLate() {
        return this == IN_PROGRESS_LATE || this == LATE || this == LATE_AND_EARLY_LEAVE;
    }

    public boolean isFinalStatus() {
        return this == NORMAL
                || this == LATE
                || this == EARLY_LEAVE
                || this == LATE_AND_EARLY_LEAVE
                || this == MISSING_CHECK_OUT
                || this == LEAVE;
    }
}
