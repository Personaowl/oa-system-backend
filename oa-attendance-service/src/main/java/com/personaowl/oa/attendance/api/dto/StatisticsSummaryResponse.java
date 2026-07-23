package com.personaowl.oa.attendance.api.dto;

import java.time.LocalDate;

public record StatisticsSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        Long departmentId,
        boolean departmentFilterApplied,
        String scopeNote,
        long totalRecords,
        long totalUsers,
        long normalCount,
        long lateCount,
        long earlyLeaveCount,
        long missingCheckOutCount
) {
}
