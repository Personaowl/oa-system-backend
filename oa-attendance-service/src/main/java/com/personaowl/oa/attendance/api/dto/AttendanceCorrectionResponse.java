package com.personaowl.oa.attendance.api.dto;

import com.personaowl.oa.attendance.domain.AttendanceCorrectionStatus;
import com.personaowl.oa.attendance.domain.AttendanceCorrectionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceCorrectionResponse(
        String id,
        String userId,
        String userName,
        LocalDate workDate,
        AttendanceCorrectionType correctionType,
        LocalDateTime correctionTime,
        String reason,
        AttendanceCorrectionStatus status,
        String approverId,
        String reviewComment,
        LocalDateTime decidedAt,
        LocalDateTime createdAt
) {
}
