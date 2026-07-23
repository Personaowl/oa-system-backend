package com.personaowl.oa.user.service;

import com.personaowl.oa.user.api.dto.DepartmentResponse;
import com.personaowl.oa.user.api.dto.UserResponse;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

@Service
public class OrganizationExcelExportService {

    private static final Map<String, String> ROLE_NAMES = Map.of(
            "ADMIN", "系统管理员",
            "HR", "HR 人事",
            "MANAGER", "部门主管",
            "EMPLOYEE", "普通员工"
    );

    public byte[] exportDepartments(List<DepartmentResponse> departments) {
        List<DepartmentResponse> rows = departments == null ? List.of() : departments;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("部门数据");
            Styles styles = createStyles(workbook);
            List<String> headers = List.of("部门ID", "部门名称", "上级部门", "负责人", "员工人数", "状态", "排序号", "创建时间", "更新时间");
            writeHeader(sheet, headers, styles.header());

            Map<Long, String> departmentNames = new HashMap<>();
            rows.forEach(item -> departmentNames.put(item.id(), item.name()));
            int rowIndex = 1;
            for (DepartmentResponse item : rows) {
                Row row = sheet.createRow(rowIndex++);
                int column = 0;
                writeText(row, column++, string(item.id()), styles.body());
                writeText(row, column++, item.name(), styles.body());
                writeText(row, column++, parentName(item.parentId(), departmentNames), styles.body());
                writeText(row, column++, managerNames(item.managerNames()), styles.body());
                writeNumber(row, column++, item.employeeCount(), styles.integer());
                writeText(row, column++, statusName(item.status()), styles.body());
                writeNumber(row, column++, item.sortOrder() == null ? 0 : item.sortOrder(), styles.integer());
                writeDate(row, column++, item.createdAt(), styles.dateTime());
                writeDate(row, column, item.updatedAt(), styles.dateTime());
            }
            finishSheet(sheet, headers.size(), new int[]{22, 24, 24, 24, 14, 12, 12, 22, 22});
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("生成部门 Excel 失败", ex);
        }
    }

    public byte[] exportUsers(List<UserResponse> users, boolean includeSalary) {
        List<UserResponse> rows = users == null ? List.of() : users;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("员工数据");
            Styles styles = createStyles(workbook);
            List<String> headers = new ArrayList<>(List.of("员工ID", "姓名", "登录账号", "部门", "角色", "联系电话", "邮箱"));
            if (includeSalary) headers.add("月基本薪资（元）");
            headers.add("状态");
            writeHeader(sheet, headers, styles.header());

            int rowIndex = 1;
            for (UserResponse item : rows) {
                Row row = sheet.createRow(rowIndex++);
                int column = 0;
                writeText(row, column++, string(item.id()), styles.body());
                writeText(row, column++, item.displayName(), styles.body());
                writeText(row, column++, item.username(), styles.body());
                writeText(row, column++, item.departmentName(), styles.body());
                writeText(row, column++, roleNames(item.roleCodes()), styles.body());
                writeText(row, column++, item.phone(), styles.body());
                writeText(row, column++, item.email(), styles.body());
                if (includeSalary) writeMoney(row, column++, item.salary(), styles.money());
                writeText(row, column, statusName(item.status()), styles.body());
            }

            int[] widths = includeSalary
                    ? new int[]{22, 18, 20, 20, 24, 20, 30, 20, 12}
                    : new int[]{22, 18, 20, 20, 24, 20, 30, 12};
            finishSheet(sheet, headers.size(), widths);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("生成员工 Excel 失败", ex);
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

        CellStyle money = workbook.createCellStyle();
        money.cloneStyleFrom(body);
        money.setAlignment(HorizontalAlignment.RIGHT);
        money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

        CellStyle dateTime = workbook.createCellStyle();
        dateTime.cloneStyleFrom(body);
        dateTime.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return new Styles(header, body, integer, money, dateTime);
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private void writeHeader(Sheet sheet, List<String> headers, CellStyle style) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(24);
        for (int index = 0; index < headers.size(); index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(headers.get(index));
            cell.setCellStyle(style);
        }
    }

    private void writeText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(safeText(value));
        cell.setCellStyle(style);
    }

    private void writeNumber(Row row, int column, long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeMoney(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? 0D : value.doubleValue());
        cell.setCellStyle(style);
    }

    private void writeDate(Row row, int column, LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void finishSheet(Sheet sheet, int columns, int[] widths) {
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columns - 1));
        for (int index = 0; index < widths.length; index++) {
            sheet.setColumnWidth(index, Math.min(255, widths[index]) * 256);
        }
    }

    private String parentName(Long parentId, Map<Long, String> departmentNames) {
        if (parentId == null || parentId == 0) return "无（根部门）";
        return departmentNames.getOrDefault(parentId, "未知部门（" + parentId + "）");
    }

    private String roleNames(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) return "未分配";
        StringJoiner joiner = new StringJoiner("、");
        roleCodes.forEach(code -> joiner.add(ROLE_NAMES.getOrDefault(code, code)));
        return joiner.toString();
    }

    private String managerNames(List<String> names) {
        return names == null || names.isEmpty() ? "未指定" : String.join("、", names);
    }

    private String statusName(Integer status) {
        return status != null && status == 1 ? "启用" : "停用";
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safeText(String value) {
        if (value == null) return "";
        String text = value.strip();
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) return "'" + text;
        return text;
    }

    private record Styles(CellStyle header, CellStyle body, CellStyle integer,
                          CellStyle money, CellStyle dateTime) {
    }
}
