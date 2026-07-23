package com.personaowl.oa.attendance.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record ShiftRequest(
        @NotBlank @Size(max = 64) String name,
        @NotNull LocalTime workStart,
        @NotNull LocalTime workEnd,
        @Min(0) @Max(180) int lateThresholdMinutes,
        @Size(max = 16) String color,
        boolean defaultShift,
        boolean enabled
) {
}
