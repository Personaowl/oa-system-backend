package com.personaowl.oa.user.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.common.web.PermissionGuard;
import com.personaowl.oa.user.api.dto.SalaryDetailUpdateRequest;
import com.personaowl.oa.user.api.dto.SalaryGradeResponse;
import com.personaowl.oa.user.api.dto.UserPageResponse;
import com.personaowl.oa.user.api.dto.UserResponse;
import com.personaowl.oa.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/salaries")
public class SalaryManagementController {
    private final UserManagementService userService;
    private final PermissionGuard permissionGuard;

    public SalaryManagementController(UserManagementService userService, PermissionGuard permissionGuard) {
        this.userService = userService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<UserPageResponse> listSalaries(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer size,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long operatorId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:salary:view");
        return ApiResponse.success(
                userService.listUsers(operatorId, roles, keyword, departmentId, page, size), traceId);
    }

    @GetMapping("/grades")
    public ApiResponse<List<SalaryGradeResponse>> listSalaryGrades(
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:salary:view");
        return ApiResponse.success(userService.listSalaryGrades(), traceId);
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateSalary(
            @PathVariable Long userId,
            @Valid @RequestBody SalaryDetailUpdateRequest request,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long operatorId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:salary:update");
        return ApiResponse.success(
                userService.updateSalaryDetail(operatorId, roles, userId, request), traceId);
    }
}
