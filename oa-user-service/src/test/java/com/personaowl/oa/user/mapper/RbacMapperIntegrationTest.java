package com.personaowl.oa.user.mapper;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.personaowl.oa.user.domain.SysPermission;
import com.personaowl.oa.user.domain.SysRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class RbacMapperIntegrationTest {
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private SysPermissionMapper permissionMapper;

    @Test
    void shouldPersistRoleAndPermissionAssignment() {
        long roleId = IdWorker.getId();
        long permissionId = IdWorker.getId();

        SysRole role = new SysRole();
        role.setId(roleId);
        role.setCode("TEST_" + roleId);
        role.setName("RBAC集成测试角色");
        role.setStatus(1);
        role.setDeleted(0);
        roleMapper.insert(role);

        SysPermission permission = new SysPermission();
        permission.setId(permissionId);
        permission.setParentId(0L);
        permission.setCode("test:permission:" + permissionId);
        permission.setName("RBAC集成测试权限");
        permission.setType("API");
        permission.setPath("/test");
        permission.setDeleted(0);
        permissionMapper.insert(permission);

        assertNotNull(roleMapper.findAvailableById(roleId));
        assertEquals(1L, roleMapper.countEnabledIds(Set.of(roleId)));
        assertEquals(1L, permissionMapper.countAvailableIds(Set.of(permissionId)));

        roleMapper.insertRolePermission(roleId, permissionId);
        assertEquals(Set.of(permissionId), Set.copyOf(roleMapper.findPermissionIds(roleId)));
    }
}
