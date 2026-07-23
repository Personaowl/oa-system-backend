package com.personaowl.oa.user.service;

import com.personaowl.oa.user.api.dto.DepartmentResponse;
import com.personaowl.oa.user.api.dto.UserResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationExcelExportServiceTest {

    private final OrganizationExcelExportService service = new OrganizationExcelExportService();

    @Test
    void departmentWorkbookPreservesLongIdAsText() throws Exception {
        long id = 2079489406225780737L;
        DepartmentResponse department = new DepartmentResponse(
                id, 0L, "研发部", 10, 1,
                LocalDateTime.of(2026, 7, 23, 9, 0),
                LocalDateTime.of(2026, 7, 23, 10, 0),
                8, List.of("MTY 部门主管"));

        byte[] content = service.exportDepartments(List.of(department));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            var sheet = workbook.getSheet("部门数据");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("部门ID");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo(String.valueOf(id));
            assertThat(sheet.getRow(1).getCell(4).getNumericCellValue()).isEqualTo(8D);
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
        }
    }

    @Test
    void userWorkbookIncludesSalaryOnlyWhenAuthorizedAndEscapesFormulaText() throws Exception {
        UserResponse user = new UserResponse(
                2079489406225780738L, 1L, "研发部", "employee01", "=危险公式",
                "13800000000", "employee@example.com", new BigDecimal("12500.50"), 1,
                Set.of(2L), Set.of("EMPLOYEE"));

        byte[] authorized = service.exportUsers(List.of(user), true);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(authorized))) {
            var sheet = workbook.getSheet("员工数据");
            assertThat(sheet.getRow(0).getCell(7).getStringCellValue()).isEqualTo("月基本薪资（元）");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("'=危险公式");
            assertThat(sheet.getRow(1).getCell(7).getNumericCellValue()).isEqualTo(12500.50D);
        }

        byte[] unauthorized = service.exportUsers(List.of(user), false);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(unauthorized))) {
            var header = workbook.getSheet("员工数据").getRow(0);
            assertThat(header.getLastCellNum()).isEqualTo((short) 8);
            assertThat(header.getCell(7).getStringCellValue()).isEqualTo("状态");
        }
    }
}
