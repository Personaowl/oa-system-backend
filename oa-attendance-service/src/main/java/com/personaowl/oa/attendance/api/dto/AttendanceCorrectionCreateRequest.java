package com.personaowl.oa.attendance.api.dto;

import com.personaowl.oa.attendance.domain.AttendanceCorrectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceCorrectionCreateRequest(
        @NotNull LocalDate workDate,
        @NotNull AttendanceCorrectionType correctionType,
        @NotNull LocalTime correctionTime,
        @NotBlank @Size(max = 500) String reason
) {
}
