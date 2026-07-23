package com.personaowl.oa.attendance.api.dto;

import java.time.LocalTime;

public record ShiftResponse(
        String id,
        String name,
        LocalTime workStart,
        LocalTime workEnd,
        int lateThresholdMinutes,
        String color,
        boolean defaultShift,
        boolean enabled
) {
}
