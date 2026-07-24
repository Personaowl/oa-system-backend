package com.personaowl.oa.attendance.support;

import com.personaowl.oa.attendance.support.AttendanceAuthorizationService.RecordQueryScope;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceScopeMapper;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttendanceAuthorizationServiceTest {

    private final AttendanceAuthorizationService service = new AttendanceAuthorizationService();

    @Test
    void defaultsEmployeeQueryToSelf() {
        OperatorContext operator = new OperatorContext(10001L, Set.of(), "trace-1");

        RecordQueryScope scope = service.resolveRecordQueryScope(operator, null, null);

        assertEquals(10001L, scope.targetUserId());
        assertEquals("SELF", scope.dataScope());
        assertFalse(scope.departmentFilterApplied());
    }

    @Test
    void allowsEmployeeToExplicitlyQuerySelf() {
        OperatorContext operator = new OperatorContext(10001L, Set.of(), "trace-1");

        RecordQueryScope scope = service.resolveRecordQueryScope(operator, 10001L, null);

        assertEquals(10001L, scope.targetUserId());
        assertEquals("SELF", scope.dataScope());
    }

    @Test
    void rejectsEmployeeQueryingAnotherUserOrDepartment() {
        OperatorContext operator = new OperatorContext(10001L, Set.of(), "trace-1");

        BusinessException otherUser = assertThrows(BusinessException.class,
                () -> service.resolveRecordQueryScope(operator, 10002L, null));
        BusinessException department = assertThrows(BusinessException.class,
                () -> service.resolveRecordQueryScope(operator, null, 10L));

        assertEquals(ErrorCode.FORBIDDEN, otherUser.errorCode());
        assertEquals(ErrorCode.FORBIDDEN, department.errorCode());
    }

    @Test
    void allowsAuthorizedAdministratorToQueryAllOrSpecificUser() {
        OperatorContext operator = new OperatorContext(
                90001L, Set.of(AttendanceAuthorizationService.RECORD_QUERY_PERMISSION,
                AttendanceAuthorizationService.ALL_DATA_SCOPE_PERMISSION), "trace-admin");

        RecordQueryScope allUsers = service.resolveRecordQueryScope(operator, null, null);
        RecordQueryScope specificUser = service.resolveRecordQueryScope(operator, 10002L, null);

        assertNull(allUsers.targetUserId());
        assertEquals("ALL_USERS", allUsers.dataScope());
        assertEquals(10002L, specificUser.targetUserId());
        assertEquals("SPECIFIC_USER", specificUser.dataScope());
    }

    @Test
    void appliesAuthorizedDepartmentFilter() {
        OperatorContext operator = new OperatorContext(
                90001L, Set.of(AttendanceAuthorizationService.RECORD_QUERY_PERMISSION,
                AttendanceAuthorizationService.ALL_DATA_SCOPE_PERMISSION), "trace-admin");

        RecordQueryScope scope = service.resolveRecordQueryScope(operator, null, 10L);

        assertEquals("DEPARTMENT", scope.dataScope());
        assertTrue(scope.departmentFilterApplied());
        assertEquals(List.of(10L), scope.departmentIds());
        assertEquals("查询指定部门考勤", scope.scopeNote());
    }

    @Test
    void limitsManagerToManagedDepartmentsAndRejectsOtherDepartment() {
        AttendanceScopeMapper mapper = mock(AttendanceScopeMapper.class);
        AttendanceAuthorizationService scopedService = new AttendanceAuthorizationService(mapper);
        OperatorContext manager = new OperatorContext(
                20001L,
                Set.of("CUSTOM_MANAGER"),
                Set.of(AttendanceAuthorizationService.RECORD_QUERY_PERMISSION,
                        AttendanceAuthorizationService.DEPARTMENT_DATA_SCOPE_PERMISSION),
                "trace-manager");
        when(mapper.findManagedDepartmentIds(20001L)).thenReturn(List.of(10L));

        RecordQueryScope scope = scopedService.resolveRecordQueryScope(manager, null, 10L);
        assertEquals("DEPARTMENT", scope.dataScope());
        assertEquals(List.of(10L), scope.departmentIds());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scopedService.resolveRecordQueryScope(manager, null, 11L));
        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode());
    }

    @Test
    void requiresStatisticsPermissionForAdministrativeSummary() {
        OperatorContext authorized = new OperatorContext(
                90001L, Set.of(AttendanceAuthorizationService.STATISTICS_QUERY_PERMISSION), "trace-admin");
        OperatorContext unauthorized = new OperatorContext(
                90002L, Set.of(AttendanceAuthorizationService.RECORD_QUERY_PERMISSION), "trace-user");

        service.requireStatisticsQuery(authorized);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.requireStatisticsQuery(unauthorized));

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode());
    }
}
