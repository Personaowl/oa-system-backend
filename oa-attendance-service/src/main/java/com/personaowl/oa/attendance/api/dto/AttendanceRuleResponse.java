package com.personaowl.oa.attendance.api.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record AttendanceRuleResponse(
        LocalTime workStart,
        LocalTime workEnd,
        int lateThresholdMinutes,
        LocalDateTime updatedAt
) {
}
