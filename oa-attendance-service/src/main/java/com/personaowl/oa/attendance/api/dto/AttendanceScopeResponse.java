package com.personaowl.oa.attendance.api.dto;

import java.util.List;

public record AttendanceScopeResponse(
        String dataScope,
        String scopeNote,
        List<DepartmentOption> departments,
        List<UserOption> users
) {
    public AttendanceScopeResponse {
        departments = List.copyOf(departments);
        users = List.copyOf(users);
    }

    public record DepartmentOption(String id, String name) {}

    public record UserOption(
            String id,
            String username,
            String displayName,
            String departmentId,
            String departmentName
    ) {}
}
