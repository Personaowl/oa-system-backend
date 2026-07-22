package com.personaowl.oa.user.service;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.common.security.JwtClaims;
import com.personaowl.oa.common.security.JwtProperties;
import com.personaowl.oa.common.security.JwtTokenService;
import com.personaowl.oa.user.api.dto.LoginRequest;
import com.personaowl.oa.user.api.dto.RegisterRequest;
import com.personaowl.oa.user.domain.SysUser;
import com.personaowl.oa.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setAccessTokenTtl(Duration.ofHours(2));
        authService = new AuthService(userMapper, passwordEncoder, jwtTokenService, properties);
    }

    @Test
    void loginReturnsTokenAndUserContext() {
        SysUser user = user();
        when(userMapper.findEnabledByUsername("admin")).thenReturn(user);
        when(passwordEncoder.matches("123456", user.getPasswordHash())).thenReturn(true);
        when(userMapper.findRoleCodes(1L)).thenReturn(List.of("ADMIN"));
        when(userMapper.findPermissionCodes(1L)).thenReturn(List.of("user:read", "system:admin"));
        when(jwtTokenService.issue(any(JwtClaims.class))).thenReturn("signed-token");

        var response = authService.login(new LoginRequest(" admin ", "123456"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.expiresIn()).isEqualTo(7200);
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().roles()).containsExactly("ADMIN");

        ArgumentCaptor<JwtClaims> claims = ArgumentCaptor.forClass(JwtClaims.class);
        verify(jwtTokenService).issue(claims.capture());
        assertThat(claims.getValue().userId()).isEqualTo(1L);
        assertThat(claims.getValue().permissions()).contains("user:read", "system:admin");
    }

    @Test
    void loginDoesNotRevealWhetherUsernameOrPasswordWasWrong() {
        SysUser user = user();
        when(userMapper.findEnabledByUsername("admin")).thenReturn(user);
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        verify(jwtTokenService, never()).issue(any());
    }

    @Test
    void currentUserLoadsFreshRolesAndPermissions() {
        when(userMapper.findEnabledById(1L)).thenReturn(user());
        when(userMapper.findRoleCodes(1L)).thenReturn(List.of("ADMIN"));
        when(userMapper.findPermissionCodes(1L)).thenReturn(List.of("user:read"));

        var response = authService.currentUser(1L);

        assertThat(response.displayName()).isEqualTo("系统管理员");
        assertThat(response.roles()).containsExactly("ADMIN");
        assertThat(response.permissions()).containsExactly("user:read");
    }

    @Test
    void registerAssignsDefaultEmployeeRoleAndReturnsPermissions() {
        when(userMapper.countByUsername("mty")).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(userMapper.findEnabledRoleIdByCode("EMPLOYEE")).thenReturn(2L);
        when(userMapper.findRoleCodes(anyLong())).thenReturn(List.of("EMPLOYEE"));
        when(userMapper.findPermissionCodes(anyLong())).thenReturn(List.of("attendance:read", "flow:read", "notice:read", "ai:chat"));

        var response = authService.register(new RegisterRequest(" mty ", "123456"));

        assertThat(response.username()).isEqualTo("mty");
        assertThat(response.roles()).containsExactly("EMPLOYEE");
        assertThat(response.permissions()).contains("attendance:read", "flow:read", "notice:read", "ai:chat");
        verify(userMapper).insertUserRole(anyLong(), eq(2L));
    }

    @Test
    void registerFailsWhenDefaultEmployeeRoleIsMissing() {
        when(userMapper.countByUsername("mty")).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(userMapper.findEnabledRoleIdByCode("EMPLOYEE")).thenReturn(null);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("mty", "123456")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.SYSTEM_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("默认员工角色未初始化");
                });
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDepartmentId(1L);
        user.setUsername("admin");
        user.setPasswordHash("bcrypt-hash");
        user.setDisplayName("系统管理员");
        user.setEmail("admin@oa.local");
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }
}
