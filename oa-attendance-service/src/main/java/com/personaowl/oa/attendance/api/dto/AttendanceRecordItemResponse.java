package com.personaowl.oa.attendance.api.dto;

import com.personaowl.oa.attendance.domain.AttendanceStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AttendanceRecordItemResponse(
        String recordId,
        String userId,
        String username,
        String employeeName,
        String departmentId,
        String departmentName,
        LocalDate workDate,
        OffsetDateTime checkInTime,
        OffsetDateTime checkOutTime,
        AttendanceStatus status,
        int lateMinutes,
        int earlyLeaveMinutes,
        int actualWorkMinutes
) {
    public AttendanceRecordItemResponse(String recordId, String userId, String username,
                                        String employeeName, String departmentId, String departmentName,
                                        LocalDate workDate, OffsetDateTime checkInTime,
                                        OffsetDateTime checkOutTime, AttendanceStatus status,
                                        int lateMinutes, int earlyLeaveMinutes) {
        this(recordId, userId, username, employeeName, departmentId, departmentName,
                workDate, checkInTime, checkOutTime, status, lateMinutes, earlyLeaveMinutes, 0);
    }

    public AttendanceRecordItemResponse(String recordId,
                                        String userId,
                                        LocalDate workDate,
                                        OffsetDateTime checkInTime,
                                        OffsetDateTime checkOutTime,
                                        AttendanceStatus status,
                                        int lateMinutes,
                                        int earlyLeaveMinutes) {
        this(recordId, userId, null, null, null, null, workDate,
                checkInTime, checkOutTime, status, lateMinutes, earlyLeaveMinutes, 0);
    }
}
