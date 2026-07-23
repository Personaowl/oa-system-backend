package com.personaowl.oa.user.api;

import com.personaowl.oa.common.web.CommonWebAutoConfiguration;
import com.personaowl.oa.user.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepartmentController.class)
@Import(CommonWebAutoConfiguration.class)
class DepartmentControllerPermissionTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private DepartmentService departmentService;

    @Test
    void listDepartmentsRejectsWrongPermission() throws Exception {
        mockMvc.perform(get("/api/v1/departments").header("X-Permissions", "notice:view"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listDepartmentsAcceptsDepartmentListPermission() throws Exception {
        when(departmentService.listDepartments()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/departments").header("X-Permissions", "sys:dept:list"))
                .andExpect(status().isOk());
    }
}
