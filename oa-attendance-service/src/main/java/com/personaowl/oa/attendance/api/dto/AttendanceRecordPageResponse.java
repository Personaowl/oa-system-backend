package com.personaowl.oa.attendance.api.dto;

import java.util.List;

public record AttendanceRecordPageResponse(
        List<AttendanceRecordItemResponse> items,
        int page,
        int size,
        long total,
        String dataScope,
        boolean departmentFilterApplied,
        String scopeNote
) {
    public AttendanceRecordPageResponse {
        items = List.copyOf(items);
    }
}
