package com.personaowl.oa.attendance.application;

import com.personaowl.oa.attendance.api.dto.AttendanceRecordItemResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AttendanceExcelExportService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] export(List<AttendanceRecordItemResponse> records) {
        List<AttendanceRecordItemResponse> rows = records == null ? List.of() : records;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("考勤记录");
            Styles styles = createStyles(workbook);
            List<String> headers = List.of("日期", "员工", "登录账号", "部门", "上班时间", "下班时间",
                    "实际工时", "工时（分钟）", "考勤状态", "迟到分钟", "早退分钟");
            writeHeader(sheet, headers, styles.header());
            int rowIndex = 1;
            for (AttendanceRecordItemResponse item : rows) {
                Row row = sheet.createRow(rowIndex++);
                int column = 0;
                writeText(row, column++, item.workDate() == null ? "" : item.workDate().toString(), styles.body());
                writeText(row, column++, item.employeeName(), styles.body());
                writeText(row, column++, item.username(), styles.body());
                writeText(row, column++, item.departmentName(), styles.body());
                writeText(row, column++, formatDateTime(item.checkInTime()), styles.body());
                writeText(row, column++, formatDateTime(item.checkOutTime()), styles.body());
                writeText(row, column++, formatWorkDuration(item.actualWorkMinutes()), styles.body());
                writeNumber(row, column++, item.actualWorkMinutes(), styles.integer());
                writeText(row, column++, statusName(item.status() == null ? null : item.status().name()), styles.body());
                writeNumber(row, column++, item.lateMinutes(), styles.integer());
                writeNumber(row, column, item.earlyLeaveMinutes(), styles.integer());
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.size() - 1));
            int[] widths = {14, 18, 20, 20, 22, 22, 16, 16, 18, 14, 14};
            for (int index = 0; index < widths.length; index++) sheet.setColumnWidth(index, widths[index] * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("生成考勤 Excel 失败", exception);
        }
    }

    private Styles createStyles(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(header);
        CellStyle body = workbook.createCellStyle();
        body.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(body);
        CellStyle integer = workbook.createCellStyle();
        integer.cloneStyleFrom(body);
        integer.setAlignment(HorizontalAlignment.RIGHT);
        integer.setDataFormat(workbook.createDataFormat().getFormat("0"));
        return new Styles(header, body, integer);
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN); style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN); style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private void writeHeader(Sheet sheet, List<String> headers, CellStyle style) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(24);
        for (int index = 0; index < headers.size(); index++) writeText(row, index, headers.get(index), style);
    }

    private void writeText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        String text = value == null ? "" : value.strip();
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        cell.setCellValue(text);
        cell.setCellStyle(style);
    }

    private void writeNumber(Row row, int column, long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String formatDateTime(OffsetDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    private String formatWorkDuration(int minutes) {
        if (minutes <= 0) return "0 分钟";
        int hours = minutes / 60;
        int remainder = minutes % 60;
        return hours == 0 ? remainder + " 分钟" : hours + " 小时" + (remainder == 0 ? "" : " " + remainder + " 分钟");
    }

    private String statusName(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "NORMAL" -> "正常";
            case "IN_PROGRESS" -> "工作中";
            case "IN_PROGRESS_LATE" -> "工作中（迟到）";
            case "LATE" -> "迟到";
            case "EARLY_LEAVE" -> "早退";
            case "LATE_AND_EARLY_LEAVE" -> "迟到且早退";
            case "MISSING_CHECK_IN" -> "缺上班卡";
            case "MISSING_CHECK_OUT" -> "缺下班卡";
            case "ABSENT" -> "旷工";
            case "LEAVE" -> "请假";
            default -> status;
        };
    }

    private record Styles(CellStyle header, CellStyle body, CellStyle integer) {}
}
