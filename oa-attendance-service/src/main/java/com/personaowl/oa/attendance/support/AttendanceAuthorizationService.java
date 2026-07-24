package com.personaowl.oa.attendance.support;

import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceScopeMapper;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AttendanceAuthorizationService {

    public static final String RECORD_QUERY_PERMISSION = "attendance:record:query";
    public static final String STATISTICS_QUERY_PERMISSION = "attendance:statistics:query";
    public static final String RULE_UPDATE_PERMISSION = "attendance:rule:update";
    public static final String SYSTEM_ADMIN_PERMISSION = "system:admin";
    public static final String ALL_DATA_SCOPE_PERMISSION = "data:scope:all";
    public static final String DEPARTMENT_DATA_SCOPE_PERMISSION = "data:scope:department";

    private final AttendanceScopeMapper scopeMapper;

    @Autowired
    public AttendanceAuthorizationService(AttendanceScopeMapper scopeMapper) {
        this.scopeMapper = scopeMapper;
    }

    /** 仅供不涉及部门范围的轻量单元测试使用。 */
    public AttendanceAuthorizationService() {
        this.scopeMapper = null;
    }

    public RecordQueryScope resolveRecordQueryScope(OperatorContext operator,
                                                    Long requestedUserId,
                                                    Long requestedDepartmentId) {
        if (!operator.permissions().contains(RECORD_QUERY_PERMISSION)) {
            return selfScope(operator, requestedUserId, requestedDepartmentId);
        }
        return resolveManagementScope(operator, requestedUserId, requestedDepartmentId);
    }

    public RecordQueryScope resolveStatisticsScope(OperatorContext operator,
                                                   Long requestedUserId,
                                                   Long requestedDepartmentId) {
        if (!operator.permissions().contains(STATISTICS_QUERY_PERMISSION)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return resolveManagementScope(operator, requestedUserId, requestedDepartmentId);
    }

    private RecordQueryScope resolveManagementScope(OperatorContext operator,
                                                    Long requestedUserId,
                                                    Long requestedDepartmentId) {
        if (hasAllDataScope(operator)) {
            return unrestrictedScope(requestedUserId, requestedDepartmentId);
        }
        if (!operator.permissions().contains(DEPARTMENT_DATA_SCOPE_PERMISSION)) {
            return selfScope(operator, requestedUserId, requestedDepartmentId);
        }

        List<Long> managedDepartmentIds = requireScopeMapper()
                .findManagedDepartmentIds(operator.userId());
        if (managedDepartmentIds == null || managedDepartmentIds.isEmpty()) {
            return selfScope(operator, requestedUserId, requestedDepartmentId);
        }

        List<Long> selectedDepartments = managedDepartmentIds;
        if (requestedDepartmentId != null) {
            if (!managedDepartmentIds.contains(requestedDepartmentId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            selectedDepartments = List.of(requestedDepartmentId);
        }
        if (requestedUserId != null
                && requireScopeMapper().countUserInDepartments(requestedUserId, selectedDepartments) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return new RecordQueryScope(
                requestedUserId,
                List.copyOf(selectedDepartments),
                requestedUserId == null ? "DEPARTMENT" : "SPECIFIC_USER",
                true,
                requestedDepartmentId == null ? "查询本人负责部门的考勤" : "查询指定负责部门的考勤");
    }

    private RecordQueryScope unrestrictedScope(Long requestedUserId, Long requestedDepartmentId) {
        return new RecordQueryScope(
                requestedUserId,
                requestedDepartmentId == null ? List.of() : List.of(requestedDepartmentId),
                requestedUserId == null
                        ? (requestedDepartmentId == null ? "ALL_USERS" : "DEPARTMENT")
                        : "SPECIFIC_USER",
                requestedDepartmentId != null,
                requestedDepartmentId == null ? "查询全部组织考勤" : "查询指定部门考勤");
    }

    private RecordQueryScope selfScope(OperatorContext operator,
                                       Long requestedUserId,
                                       Long requestedDepartmentId) {
        if (requestedDepartmentId != null
                || (requestedUserId != null && requestedUserId != operator.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return new RecordQueryScope(
                operator.userId(), List.of(), "SELF", false, "仅查询当前用户本人记录");
    }

    private boolean hasAllDataScope(OperatorContext operator) {
        return operator.permissions().contains(SYSTEM_ADMIN_PERMISSION)
                || operator.permissions().contains(ALL_DATA_SCOPE_PERMISSION);
    }

    private AttendanceScopeMapper requireScopeMapper() {
        if (scopeMapper == null) {
            throw new IllegalStateException("AttendanceScopeMapper is required for department data scope");
        }
        return scopeMapper;
    }

    public void requireStatisticsQuery(OperatorContext operator) {
        if (!operator.permissions().contains(STATISTICS_QUERY_PERMISSION)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    public void requireRuleUpdate(OperatorContext operator) {
        if (!operator.permissions().contains(RULE_UPDATE_PERMISSION)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    public record RecordQueryScope(
            Long targetUserId,
            List<Long> departmentIds,
            String dataScope,
            boolean departmentFilterApplied,
            String scopeNote
    ) {
        public RecordQueryScope {
            departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
        }
    }
}
