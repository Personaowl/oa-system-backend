package com.personaowl.oa.attendance.support;

import com.personaowl.oa.attendance.support.AttendanceAuthorizationService.RecordQueryScope;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                90001L, Set.of(AttendanceAuthorizationService.RECORD_QUERY_PERMISSION), "trace-admin");

        RecordQueryScope allUsers = service.resolveRecordQueryScope(operator, null, null);
        RecordQueryScope specificUser = service.resolveRecordQueryScope(operator, 10002L, null);

        assertNull(allUsers.targetUserId());
        assertEquals("ALL_USERS", allUsers.dataScope());
        assertEquals(10002L, specificUser.targetUserId());
        assertEquals("SPECIFIC_USER", specificUser.dataScope());
    }

    @Test
    void explainsThatAuthorizedDepartmentFilterIsReserved() {
        OperatorContext operator = new OperatorContext(
                90001L, Set.of(AttendanceAuthorizationService.RECORD_QUERY_PERMISSION), "trace-admin");

        RecordQueryScope scope = service.resolveRecordQueryScope(operator, null, 10L);

        assertEquals("ALL_USERS", scope.dataScope());
        assertFalse(scope.departmentFilterApplied());
        assertEquals("departmentId 本期仅预留，未参与数据过滤", scope.scopeNote());
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
