package com.personaowl.oa.document.application;

import com.personaowl.oa.document.domain.DocumentUserScope;
import com.personaowl.oa.document.infrastructure.DocumentAccessMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAccessServiceTest {

    @Mock
    private DocumentAccessMapper accessMapper;

    @InjectMocks
    private DocumentAccessService accessService;

    @Test
    void employeeCanAccessOwnDepartmentAndManageAssignedDepartment() {
        DocumentUserScope user = user(10L, 100L);
        when(accessMapper.findUserScope(10L)).thenReturn(user);
        when(accessMapper.findManagedDepartmentIds(10L)).thenReturn(List.of(200L));

        DocumentAccessService.AccessSnapshot access =
            accessService.resolve(10L, "EMPLOYEE", "");

        assertThat(access.canAccess(100L)).isTrue();
        assertThat(access.canAccess(200L)).isTrue();
        assertThat(access.canManage(100L)).isFalse();
        assertThat(access.canManage(200L)).isTrue();
        assertThat(access.canAccess(300L)).isFalse();
    }

    @Test
    void administratorCanAccessAndManageEveryDepartment() {
        DocumentUserScope user = user(1L, 100L);
        when(accessMapper.findUserScope(1L)).thenReturn(user);
        when(accessMapper.findManagedDepartmentIds(1L)).thenReturn(List.of());
        when(accessMapper.findAllDepartmentIds()).thenReturn(List.of(100L, 200L, 300L));

        DocumentAccessService.AccessSnapshot access =
            accessService.resolve(1L, "ADMIN", "system:admin");

        assertThat(access.accessibleDepartmentIds()).containsExactly(100L, 200L, 300L);
        assertThat(access.canManage(300L)).isTrue();
    }

    private DocumentUserScope user(Long userId, Long departmentId) {
        DocumentUserScope user = new DocumentUserScope();
        user.setUserId(userId);
        user.setDepartmentId(departmentId);
        user.setDisplayName("测试用户");
        return user;
    }
}
