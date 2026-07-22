package com.personaowl.oa.attendance.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceRuleCalculatorTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final RuleSnapshot RULE = new RuleSnapshot(LocalTime.of(9, 0), LocalTime.of(18, 0), 5);

    private final AttendanceRuleCalculator calculator = new AttendanceRuleCalculator();

    @Test
    void treatsExactLateThresholdAsOnTime() {
        CheckResult result = calculator.resolveCheckIn(at("2026-07-21T01:05:00Z"), RULE);

        assertEquals(AttendanceStatus.IN_PROGRESS, result.status());
        assertFalse(result.late());
        assertEquals(0, result.lateMinutes());
    }

    @Test
    void treatsOneSecondAfterThresholdAsOneMinuteLate() {
        CheckResult result = calculator.resolveCheckIn(at("2026-07-21T01:05:01Z"), RULE);

        assertEquals(AttendanceStatus.IN_PROGRESS_LATE, result.status());
        assertTrue(result.late());
        assertEquals(1, result.lateMinutes());
    }

    @Test
    void treatsExactWorkEndAsNormalCheckOut() {
        CheckResult result = calculator.resolveCheckOut(at("2026-07-21T10:00:00Z"), false, RULE);

        assertEquals(AttendanceStatus.NORMAL, result.status());
        assertFalse(result.earlyLeave());
        assertEquals(0, result.earlyLeaveMinutes());
    }

    @Test
    void marksOnTimeEmployeeAsEarlyLeaveBeforeWorkEnd() {
        CheckResult result = calculator.resolveCheckOut(at("2026-07-21T09:59:59Z"), false, RULE);

        assertEquals(AttendanceStatus.EARLY_LEAVE, result.status());
        assertTrue(result.earlyLeave());
        assertEquals(1, result.earlyLeaveMinutes());
    }

    @Test
    void mergesLateAndEarlyLeaveIntoCombinedStatus() {
        CheckResult result = calculator.resolveCheckOut(at("2026-07-21T09:59:59Z"), true, RULE);

        assertEquals(AttendanceStatus.LATE_AND_EARLY_LEAVE, result.status());
        assertTrue(result.late());
        assertTrue(result.earlyLeave());
        assertEquals(1, result.earlyLeaveMinutes());
    }

    @Test
    void keepsLateStatusWhenLateEmployeeChecksOutAfterWorkEnd() {
        CheckResult result = calculator.resolveCheckOut(at("2026-07-21T10:30:00Z"), true, RULE);

        assertEquals(AttendanceStatus.LATE, result.status());
        assertFalse(result.earlyLeave());
    }

    @Test
    void calculatesEachSideOfMidnightAsItsOwnLocalWorkDay() {
        CheckResult beforeMidnight = calculator.resolveCheckIn(at("2026-07-21T15:59:59Z"), RULE);
        CheckResult afterMidnight = calculator.resolveCheckIn(at("2026-07-21T16:00:00Z"), RULE);

        assertEquals(AttendanceStatus.IN_PROGRESS_LATE, beforeMidnight.status());
        assertEquals(AttendanceStatus.IN_PROGRESS, afterMidnight.status());
        assertEquals(LocalDateTime.of(2026, 7, 21, 23, 59, 59), at("2026-07-21T15:59:59Z"));
        assertEquals(LocalDateTime.of(2026, 7, 22, 0, 0), at("2026-07-21T16:00:00Z"));
    }

    @Test
    void rejectsInvalidRuleSnapshots() {
        assertThrows(IllegalArgumentException.class,
                () -> new RuleSnapshot(LocalTime.of(9, 0), LocalTime.of(18, 0), -1));
        assertThrows(IllegalArgumentException.class,
                () -> new RuleSnapshot(LocalTime.of(9, 0), LocalTime.of(9, 0), 5));
    }

    private LocalDateTime at(String instant) {
        Clock fixedClock = Clock.fixed(Instant.parse(instant), ZONE_ID);
        return LocalDateTime.now(fixedClock);
    }
}
