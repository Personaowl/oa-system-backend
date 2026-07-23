package com.personaowl.oa.attendance.application;

import com.personaowl.oa.attendance.api.dto.AttendanceScopeResponse;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceDepartmentEntry;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceScopeMapper;
import com.personaowl.oa.attendance.infrastructure.persistence.AttendanceUserDirectoryEntry;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService;
import com.personaowl.oa.attendance.support.AttendanceAuthorizationService.RecordQueryScope;
import com.personaowl.oa.attendance.support.OperatorContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttendanceScopeService {

    private final AttendanceAuthorizationService authorizationService;
    private final AttendanceScopeMapper scopeMapper;

    public AttendanceScopeService(AttendanceAuthorizationService authorizationService,
                                  AttendanceScopeMapper scopeMapper) {
        this.authorizationService = authorizationService;
        this.scopeMapper = scopeMapper;
    }

    @Transactional(readOnly = true)
    public AttendanceScopeResponse getScope(OperatorContext operator) {
        RecordQueryScope scope = authorizationService.resolveRecordQueryScope(operator, null, null);
        List<AttendanceDepartmentEntry> departments;
        List<AttendanceUserDirectoryEntry> users;
        if ("ALL_USERS".equals(scope.dataScope())) {
            departments = scopeMapper.findAllDepartments();
            users = scopeMapper.findAllUsers();
        } else if ("DEPARTMENT".equals(scope.dataScope())) {
            departments = scopeMapper.findDepartmentsByIds(scope.departmentIds());
            users = scopeMapper.findUsersByDepartmentIds(scope.departmentIds());
        } else {
            AttendanceDepartmentEntry department = scopeMapper.findDepartmentByUserId(operator.userId());
            AttendanceUserDirectoryEntry user = scopeMapper.findUserById(operator.userId());
            departments = department == null ? List.of() : List.of(department);
            users = user == null ? List.of() : List.of(user);
        }
        return new AttendanceScopeResponse(
                scope.dataScope(),
                scope.scopeNote(),
                departments.stream().map(item -> new AttendanceScopeResponse.DepartmentOption(
                        String.valueOf(item.getId()), item.getName())).toList(),
                users.stream().map(item -> new AttendanceScopeResponse.UserOption(
                        String.valueOf(item.getId()),
                        item.getUsername(),
                        item.getDisplayName(),
                        item.getDepartmentId() == null ? null : String.valueOf(item.getDepartmentId()),
                        item.getDepartmentName())).toList());
    }
}
