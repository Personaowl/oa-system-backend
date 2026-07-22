package com.personaowl.oa.attendance.domain;

import java.time.LocalTime;
import java.util.Objects;

public record RuleSnapshot(
        LocalTime workStart,
        LocalTime workEnd,
        int lateThresholdMinutes
) {
    public RuleSnapshot {
        Objects.requireNonNull(workStart, "workStart must not be null");
        Objects.requireNonNull(workEnd, "workEnd must not be null");
        if (lateThresholdMinutes < 0) {
            throw new IllegalArgumentException("lateThresholdMinutes must not be negative");
        }
        if (!workEnd.isAfter(workStart)) {
            throw new IllegalArgumentException("workEnd must be after workStart for the current single-day schedule");
        }
    }

    public LocalTime latestOnTimeCheckIn() {
        return workStart.plusMinutes(lateThresholdMinutes);
    }
}
