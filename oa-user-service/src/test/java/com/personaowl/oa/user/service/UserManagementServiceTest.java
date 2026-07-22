package com.personaowl.oa.user.service;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.user.api.dto.UserCreateRequest;
import com.personaowl.oa.user.api.dto.UserUpdateRequest;
import com.personaowl.oa.user.domain.SysDepartment;
import com.personaowl.oa.user.domain.SysUser;
import com.personaowl.oa.user.mapper.SysDepartmentMapper;
import com.personaowl.oa.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {
    @Mock SysUserMapper userMapper;
    @Mock SysDepartmentMapper departmentMapper;
    @Mock RbacService rbacService;
    @Mock PasswordEncoder passwordEncoder;

    private UserManagementService service;

    @BeforeEach
    void setUp() {
        service = new UserManagementService(userMapper, departmentMapper, rbacService, passwordEncoder);
    }

    @Test
    void createUserPersistsAccountAndRoles() {
        when(departmentMapper.findAvailableById(1L)).thenReturn(department());
        when(userMapper.countByUsername("alice")).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);
        when(userMapper.findRoleIds(anyLong())).thenReturn(List.of(2L));
        when(userMapper.findRoleCodes(anyLong())).thenReturn(List.of("EMPLOYEE"));

        var response = service.createUser(new UserCreateRequest(
                " alice ", "123456", " Alice ", 1L, "13800000000", null, 1, Set.of(2L)));

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.displayName()).isEqualTo("Alice");
        assertThat(response.departmentName()).isEqualTo("总部");
        assertThat(response.roleCodes()).containsExactly("EMPLOYEE");
        verify(rbacService).assignUserRoles(response.id(), Set.of(2L));
    }

    @Test
    void updateUserCanChangeDepartmentAndRolesWithoutResettingPassword() {
        SysUser user = user(10L, "alice");
        when(userMapper.findAvailableById(10L)).thenReturn(user);
        when(userMapper.countByUsernameExcluding("alice-new", 10L)).thenReturn(0L);
        when(departmentMapper.findAvailableById(1L)).thenReturn(department());
        when(userMapper.updateById(user)).thenReturn(1);
        when(userMapper.findRoleIds(10L)).thenReturn(List.of(4L));
        when(userMapper.findRoleCodes(10L)).thenReturn(List.of("MANAGER"));

        var response = service.updateUser(10L, new UserUpdateRequest(
                "alice-new", null, "Alice", 1L, null, null, 1, Set.of(4L)));

        assertThat(response.username()).isEqualTo("alice-new");
        assertThat(response.roleCodes()).containsExactly("MANAGER");
        verify(rbacService).assignUserRoles(10L, Set.of(4L));
    }

    @Test
    void deleteUserRejectsCurrentAccount() {
        assertThatThrownBy(() -> service.deleteUser(10L, 10L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION));
    }

    private SysDepartment department() {
        SysDepartment department = new SysDepartment();
        department.setId(1L);
        department.setName("总部");
        return department;
    }

    private SysUser user(Long id, String username) {
        SysUser user = new SysUser();
        user.setId(id); user.setUsername(username); user.setDisplayName("Alice");
        user.setDepartmentId(1L); user.setPasswordHash("old-hash"); user.setStatus(1); user.setDeleted(0);
        return user;
    }
}
