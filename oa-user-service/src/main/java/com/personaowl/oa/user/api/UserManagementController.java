package com.personaowl.oa.user.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.common.web.PermissionGuard;
import com.personaowl.oa.user.api.dto.UserCreateRequest;
import com.personaowl.oa.user.api.dto.SalaryUpdateRequest;
import com.personaowl.oa.user.api.dto.UserPageResponse;
import com.personaowl.oa.user.api.dto.UserResponse;
import com.personaowl.oa.user.api.dto.UserUpdateRequest;
import com.personaowl.oa.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserManagementController {
    private final UserManagementService userService;
    private final PermissionGuard permissionGuard;

    public UserManagementController(UserManagementService userService, PermissionGuard permissionGuard) {
        this.userService = userService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<UserPageResponse> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long operatorId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:user:list");
        return ApiResponse.success(userService.listUsers(operatorId, roles, keyword, departmentId, page, size), traceId);
    }

    @PostMapping
    public ApiResponse<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:user:create");
        return ApiResponse.success(userService.createUser(request), traceId);
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:user:update");
        return ApiResponse.success(userService.updateUser(id, request), traceId);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long operatorId,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:user:delete");
        userService.deleteUser(id, operatorId);
        return ApiResponse.success(null, traceId);
    }

    @PutMapping("/{id}/salary")
    public ApiResponse<UserResponse> updateSalary(
            @PathVariable Long id,
            @Valid @RequestBody SalaryUpdateRequest request,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long operatorId,
            @RequestHeader(value = RequestHeaders.ROLES, required = false) String roles,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:salary:update");
        return ApiResponse.success(userService.updateSalary(operatorId, roles, id, request.salary()), traceId);
    }
}
