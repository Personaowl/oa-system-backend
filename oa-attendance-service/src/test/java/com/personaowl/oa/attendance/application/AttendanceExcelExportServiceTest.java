package com.personaowl.oa.attendance.application;

import com.personaowl.oa.attendance.api.dto.AttendanceRecordItemResponse;
import com.personaowl.oa.attendance.domain.AttendanceStatus;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceExcelExportServiceTest {
    @Test
    void exportsWorkDurationAndEmployeeInformation() throws Exception {
        AttendanceRecordItemResponse row = new AttendanceRecordItemResponse(
                "1", "10004", "mty-employee", "MTY 普通员工", "2002", "研发部",
                LocalDate.of(2026, 7, 24),
                OffsetDateTime.of(2026, 7, 24, 9, 0, 0, 0, ZoneOffset.ofHours(8)),
                OffsetDateTime.of(2026, 7, 24, 18, 5, 0, 0, ZoneOffset.ofHours(8)),
                AttendanceStatus.NORMAL, 0, 0, 545);

        byte[] content = new AttendanceExcelExportService().export(List.of(row));

        assertTrue(content.length > 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            var sheet = workbook.getSheet("考勤记录");
            assertEquals("实际工时", sheet.getRow(0).getCell(6).getStringCellValue());
            assertEquals("9 小时 5 分钟", sheet.getRow(1).getCell(6).getStringCellValue());
            assertEquals(545D, sheet.getRow(1).getCell(7).getNumericCellValue());
            assertEquals("MTY 普通员工", sheet.getRow(1).getCell(1).getStringCellValue());
        }
    }
}
