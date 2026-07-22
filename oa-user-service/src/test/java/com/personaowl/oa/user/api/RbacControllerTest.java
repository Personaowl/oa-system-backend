package com.personaowl.oa.user.api;

import com.personaowl.oa.common.web.CommonWebAutoConfiguration;
import com.personaowl.oa.user.api.dto.RoleResponse;
import com.personaowl.oa.user.service.RbacService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacController.class)
@Import(CommonWebAutoConfiguration.class)
class RbacControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private RbacService rbacService;

    @Test
    void listRolesRejectsMissingPermission() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    void listRolesAcceptsSpecificPermission() throws Exception {
        when(rbacService.listRoles()).thenReturn(List.of(
                new RoleResponse(2079489406225780737L, "ADMIN", "管理员", 1, Set.of(24L))));

        mockMvc.perform(get("/api/v1/roles").header("X-Permissions", "sys:role:list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("2079489406225780737"));
    }

    @Test
    void systemAdminCanAccessRbacManagement() throws Exception {
        when(rbacService.listRoles()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/roles").header("X-Permissions", "system:admin"))
                .andExpect(status().isOk());
    }
}
