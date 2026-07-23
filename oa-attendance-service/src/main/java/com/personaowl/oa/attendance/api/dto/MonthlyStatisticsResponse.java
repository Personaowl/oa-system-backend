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
        long missingCheckOutCount,
        long missingCheckInCount,
        long absentCount
) {
    public MonthlyStatisticsResponse(
            String userId, YearMonth month, LocalDate startDate, LocalDate endDate,
            long totalRecords, long normalCount, long lateCount, long earlyLeaveCount,
            long missingCheckOutCount) {
        this(userId, month, startDate, endDate, totalRecords, normalCount, lateCount,
                earlyLeaveCount, missingCheckOutCount, 0, 0);
    }
}
