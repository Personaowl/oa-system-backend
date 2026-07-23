package com.personaowl.oa.user.api;

import com.personaowl.oa.common.web.CommonWebAutoConfiguration;
import com.personaowl.oa.user.service.DepartmentService;
import com.personaowl.oa.user.service.OrganizationExcelExportService;
import com.personaowl.oa.user.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationExportController.class)
@Import(CommonWebAutoConfiguration.class)
class OrganizationExportControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean DepartmentService departmentService;
    @MockBean UserManagementService userService;
    @MockBean OrganizationExcelExportService excelExportService;

    @Test
    void departmentExportRequiresListPermission() throws Exception {
        mockMvc.perform(get("/api/v1/departments/export").header("X-Permissions", "notice:view"))
                .andExpect(status().isForbidden());
    }

    @Test
    void departmentExportReturnsXlsxAttachment() throws Exception {
        when(departmentService.listDepartmentsForExport()).thenReturn(List.of());
        when(excelExportService.exportDepartments(anyList())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/departments/export").header("X-Permissions", "sys:dept:list"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("departments.xlsx")));
    }

    @Test
    void employeeExportIncludesSalaryOnlyWithSalaryPermission() throws Exception {
        when(userService.listUsersForExport(10L, "MANAGER", null, null)).thenReturn(List.of());
        when(excelExportService.exportUsers(anyList(), org.mockito.ArgumentMatchers.eq(false))).thenReturn(new byte[]{1});

        mockMvc.perform(get("/api/v1/users/export")
                        .header("X-User-Id", "10")
                        .header("X-Roles", "MANAGER")
                        .header("X-Permissions", "sys:user:list"))
                .andExpect(status().isOk());

        verify(excelExportService).exportUsers(anyList(), org.mockito.ArgumentMatchers.eq(false));
    }
}
