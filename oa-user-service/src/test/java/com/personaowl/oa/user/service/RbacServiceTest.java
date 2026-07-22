package com.personaowl.oa.user.service;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.user.api.dto.RoleCreateRequest;
import com.personaowl.oa.user.domain.SysRole;
import com.personaowl.oa.user.domain.SysUser;
import com.personaowl.oa.user.mapper.SysPermissionMapper;
import com.personaowl.oa.user.mapper.SysRoleMapper;
import com.personaowl.oa.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {
    @Mock private SysRoleMapper roleMapper;
    @Mock private SysPermissionMapper permissionMapper;
    @Mock private SysUserMapper userMapper;
    private RbacService service;

    @BeforeEach
    void setUp() {
        service = new RbacService(roleMapper, permissionMapper, userMapper);
    }

    @Test
    void createRoleNormalizesCodeAndUsesDefaults() {
        when(roleMapper.countCodeExcluding("MANAGER", 0L)).thenReturn(0L);
        when(roleMapper.insert(any(SysRole.class))).thenReturn(1);

        var response = service.createRole(new RoleCreateRequest("manager", "经理", null));

        assertEquals("MANAGER", response.code());
        assertEquals(1, response.status());
        verify(roleMapper).insert(any(SysRole.class));
    }

    @Test
    void assignPermissionsReplacesExistingAssignments() {
        SysRole role = role(2L);
        when(roleMapper.findAvailableById(2L)).thenReturn(role);
        when(permissionMapper.countAvailableIds(Set.of(10L, 11L))).thenReturn(2L);

        var response = service.assignPermissions(2L, Set.of(10L, 11L));

        assertEquals(Set.of(10L, 11L), response.permissionIds());
        verify(roleMapper).deleteRolePermissions(2L);
        verify(roleMapper).insertRolePermission(2L, 10L);
        verify(roleMapper).insertRolePermission(2L, 11L);
    }

    @Test
    void assignPermissionsRejectsUnknownPermission() {
        when(roleMapper.findAvailableById(2L)).thenReturn(role(2L));
        when(permissionMapper.countAvailableIds(Set.of(99L))).thenReturn(0L);

        assertThrows(BusinessException.class, () -> service.assignPermissions(2L, Set.of(99L)));
    }

    @Test
    void assignUserRolesReplacesExistingAssignments() {
        SysUser user = new SysUser();
        user.setId(8L);
        when(userMapper.findAvailableById(8L)).thenReturn(user);
        when(roleMapper.countEnabledIds(Set.of(2L, 3L))).thenReturn(2L);

        assertEquals(Set.of(2L, 3L), service.assignUserRoles(8L, Set.of(2L, 3L)));
        verify(userMapper).deleteUserRoles(8L);
        verify(userMapper).insertUserRole(8L, 2L);
        verify(userMapper).insertUserRole(8L, 3L);
    }

    @Test
    void deleteRoleRejectsAssignedRole() {
        when(roleMapper.findAvailableById(2L)).thenReturn(role(2L));
        when(roleMapper.countAssignedUsers(2L)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.deleteRole(2L));
    }

    @Test
    void listRolesIncludesPermissionIds() {
        when(roleMapper.findAllAvailable()).thenReturn(List.of(role(2L)));
        when(roleMapper.findPermissionIds(2L)).thenReturn(List.of(10L, 11L));

        assertEquals(Set.of(10L, 11L), service.listRoles().getFirst().permissionIds());
    }

    private SysRole role(long id) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setCode("MANAGER");
        role.setName("经理");
        role.setStatus(1);
        role.setDeleted(0);
        return role;
    }
}
