package com.personaowl.oa.attendance.application;

import com.personaowl.oa.attendance.api.dto.AttendanceRuleUpdateRequest;
import com.personaowl.oa.attendance.config.AttendanceProperties;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRuleEntity;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceRuleMapper;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService;
import com.personaowl.oa.attendance.support.OperatorContext;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceRuleServiceTest {
    @Mock
    private AttendanceRuleMapper ruleMapper;

    private AttendanceRuleService service;

    @BeforeEach
    void setUp() {
        AttendanceProperties properties = new AttendanceProperties();
        service = new AttendanceRuleService(
                ruleMapper, properties, new AttendanceAuthorizationService());
    }

    @Test
    void fallsBackToConfiguredDefaultsBeforeDatabaseInitialization() {
        when(ruleMapper.selectById(1L)).thenReturn(null);

        var response = service.getRule();

        assertEquals(LocalTime.of(9, 0), response.workStart());
        assertEquals(LocalTime.of(18, 0), response.workEnd());
        assertEquals(5, response.lateThresholdMinutes());
        assertNull(response.updatedAt());
    }

    @Test
    void rejectsRuleUpdateWithoutDedicatedPermission() {
        OperatorContext employee = new OperatorContext(10001L, Set.of(), "trace-user");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateRule(employee,
                        new AttendanceRuleUpdateRequest(LocalTime.of(8, 30), LocalTime.of(17, 30), 10)));

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode());
        verifyNoInteractions(ruleMapper);
    }

    @Test
    void administratorCanCreatePersistentRule() {
        OperatorContext admin = new OperatorContext(1L,
                Set.of(AttendanceAuthorizationService.RULE_UPDATE_PERMISSION), "trace-admin");
        when(ruleMapper.selectById(1L)).thenReturn(null);
        when(ruleMapper.insert(any(AttendanceRuleEntity.class))).thenReturn(1);

        var response = service.updateRule(admin,
                new AttendanceRuleUpdateRequest(LocalTime.of(8, 30), LocalTime.of(17, 30), 10));

        assertEquals(LocalTime.of(8, 30), response.workStart());
        assertEquals(10, response.lateThresholdMinutes());
        verify(ruleMapper).insert(any(AttendanceRuleEntity.class));
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {
        OperatorContext admin = new OperatorContext(1L,
                Set.of(AttendanceAuthorizationService.RULE_UPDATE_PERMISSION), "trace-admin");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateRule(admin,
                        new AttendanceRuleUpdateRequest(LocalTime.of(18, 0), LocalTime.of(9, 0), 5)));

        assertEquals(ErrorCode.INVALID_ARGUMENT, exception.errorCode());
        verifyNoInteractions(ruleMapper);
    }
}
