package com.personaowl.oa.attendance.domain;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Component
public class AttendanceRuleCalculator {

    public CheckResult resolveCheckIn(LocalDateTime checkInTime, RuleSnapshot snapshot) {
        Objects.requireNonNull(checkInTime, "checkInTime must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");

        LocalTime actualTime = checkInTime.toLocalTime();
        LocalTime latestOnTime = snapshot.latestOnTimeCheckIn();
        boolean late = actualTime.isAfter(latestOnTime);
        int lateMinutes = late ? roundedUpMinutes(Duration.between(latestOnTime, actualTime)) : 0;
        AttendanceStatus status = late ? AttendanceStatus.IN_PROGRESS_LATE : AttendanceStatus.IN_PROGRESS;
        return new CheckResult(status, late, lateMinutes, false, 0);
    }

    public CheckResult resolveCheckOut(LocalDateTime checkOutTime, boolean wasLate, RuleSnapshot snapshot) {
        Objects.requireNonNull(checkOutTime, "checkOutTime must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");

        LocalTime actualTime = checkOutTime.toLocalTime();
        boolean earlyLeave = actualTime.isBefore(snapshot.workEnd());
        int earlyLeaveMinutes = earlyLeave
                ? roundedUpMinutes(Duration.between(actualTime, snapshot.workEnd()))
                : 0;

        AttendanceStatus status;
        if (wasLate && earlyLeave) {
            status = AttendanceStatus.LATE_AND_EARLY_LEAVE;
        } else if (wasLate) {
            status = AttendanceStatus.LATE;
        } else if (earlyLeave) {
            status = AttendanceStatus.EARLY_LEAVE;
        } else {
            status = AttendanceStatus.NORMAL;
        }
        return new CheckResult(status, wasLate, 0, earlyLeave, earlyLeaveMinutes);
    }

    private int roundedUpMinutes(Duration duration) {
        long seconds = duration.getSeconds();
        return Math.toIntExact((seconds + 59) / 60);
    }
}
