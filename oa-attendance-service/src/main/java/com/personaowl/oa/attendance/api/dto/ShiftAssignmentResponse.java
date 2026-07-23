package com.personaowl.oa.attendance.api.dto;

import java.time.LocalDate;

public record ShiftAssignmentResponse(
        String id,
        String userId,
        String shiftId,
        LocalDate startDate,
        LocalDate endDate
) {
}
