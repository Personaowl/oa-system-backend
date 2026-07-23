package com.personaowl.oa.attendance.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AttendanceCorrectionReviewRequest(
        @NotBlank @Pattern(regexp = "APPROVE|REJECT") String decision,
        @Size(max = 500) String comment
) {
}
