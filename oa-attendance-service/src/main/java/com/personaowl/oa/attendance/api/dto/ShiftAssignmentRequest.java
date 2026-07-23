package com.personaowl.oa.attendance.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ShiftAssignmentRequest(
        @NotNull @Min(1) Long userId,
        @NotNull @Min(1) Long shiftId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
