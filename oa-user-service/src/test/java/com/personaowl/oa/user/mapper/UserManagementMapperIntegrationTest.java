package com.personaowl.oa.user.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class UserManagementMapperIntegrationTest {
    @Autowired SysUserMapper userMapper;
    @Autowired SysDepartmentMapper departmentMapper;

    @Test
    void queriesUsersAndDepartmentOrganizationStatistics() {
        assertThat(userMapper.countAvailable(null, null)).isPositive();
        assertThat(userMapper.findAvailablePage(null, null, 0, 10)).isNotEmpty();
        assertThat(departmentMapper.countUsers(1L)).isGreaterThanOrEqualTo(0);
        assertThat(departmentMapper.findManagerNames(1L)).isNotNull();
    }
}
