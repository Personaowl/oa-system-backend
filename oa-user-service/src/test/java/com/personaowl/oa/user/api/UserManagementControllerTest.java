package com.personaowl.oa.user.api;

import com.personaowl.oa.common.web.CommonWebAutoConfiguration;
import com.personaowl.oa.user.api.dto.UserPageResponse;
import com.personaowl.oa.user.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserManagementController.class)
@Import(CommonWebAutoConfiguration.class)
class UserManagementControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean UserManagementService userService;

    @Test
    void listRequiresPermission() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("X-Permissions", "notice:read"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    void listReturnsPageWithPermission() throws Exception {
        when(userService.listUsers(any(), any(), any(), any(), eq(1), eq(20))).thenReturn(new UserPageResponse(0, List.of()));
        mockMvc.perform(get("/api/v1/users").header("X-Permissions", "sys:user:list"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
    }
}
