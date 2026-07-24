package com.personaowl.oa.attendance.api;

import com.personaowl.oa.attendance.api.dto.AttendanceRecordItemResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRecordPageResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRecordQuery;
import com.personaowl.oa.attendance.application.AttendanceApplicationService;
import com.personaowl.oa.attendance.application.AttendanceExcelExportService;
import com.personaowl.oa.attendance.domain.AttendanceStatus;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.web.RequestHeaders;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
public class AttendanceExportController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final AttendanceApplicationService attendanceService;
    private final AttendanceExcelExportService excelExportService;

    public AttendanceExportController(AttendanceApplicationService attendanceService,
                                      AttendanceExcelExportService excelExportService) {
        this.attendanceService = attendanceService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/api/v1/attendance/records/export")
    public ResponseEntity<byte[]> export(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) String operatorUserId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long departmentId) {
        OperatorContext operator = OperatorContext.fromHeaders(operatorUserId, roles, permissions, traceId);
        List<AttendanceRecordItemResponse> rows = new ArrayList<>();
        int page = 1;
        long total;
        do {
            AttendanceRecordPageResponse result = attendanceService.getRecords(operator,
                    new AttendanceRecordQuery(startDate, endDate, status, page, 100, userId, departmentId));
            total = result.total();
            if (result.items().isEmpty()) {
                break;
            }
            rows.addAll(result.items());
            page++;
        } while (rows.size() < total);

        String fileName = "考勤记录_" + FILE_TIME.format(LocalDateTime.now()) + ".xlsx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"attendance.xlsx\"; filename*=UTF-8''" + encoded)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(excelExportService.export(rows));
    }
}
