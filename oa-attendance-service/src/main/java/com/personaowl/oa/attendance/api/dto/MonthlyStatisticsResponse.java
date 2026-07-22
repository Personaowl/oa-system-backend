package com.personaowl.oa.attendance.api.dto;

import java.time.LocalDate;
import java.time.YearMonth;

public record MonthlyStatisticsResponse(
        String userId,
        YearMonth month,
        LocalDate startDate,
        LocalDate endDate,
        long totalRecords,
        long normalCount,
        long lateCount,
        long earlyLeaveCount,
        long missingCheckOutCount
) {
}
