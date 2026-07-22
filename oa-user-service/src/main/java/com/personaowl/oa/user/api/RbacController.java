package com.personaowl.oa.user.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.common.web.PermissionGuard;
import com.personaowl.oa.user.api.dto.IdSetRequest;
import com.personaowl.oa.user.api.dto.PermissionResponse;
import com.personaowl.oa.user.api.dto.RoleCreateRequest;
import com.personaowl.oa.user.api.dto.RoleResponse;
import com.personaowl.oa.user.api.dto.RoleUpdateRequest;
import com.personaowl.oa.user.service.RbacService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
public class RbacController {
    private final RbacService rbacService;
    private final PermissionGuard permissionGuard;

    public RbacController(RbacService rbacService, PermissionGuard permissionGuard) {
        this.rbacService = rbacService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> listRoles(
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:role:list");
        return ApiResponse.success(rbacService.listRoles(), traceId);
    }

    @GetMapping("/roles/{id}")
    public ApiResponse<RoleResponse> getRole(
            @PathVariable Long id,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:role:view");
        return ApiResponse.success(rbacService.getRole(id), traceId);
    }

    @PostMapping("/roles")
    public ApiResponse<RoleResponse> createRole(
            @Valid @RequestBody RoleCreateRequest request,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:role:create");
        return ApiResponse.success(rbacService.createRole(request), traceId);
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<RoleResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:role:update");
        return ApiResponse.success(rbacService.updateRole(id, request), traceId);
    }

    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(
            @PathVariable Long id,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:role:delete");
        rbacService.deleteRole(id);
        return ApiResponse.success(null, traceId);
    }

    @PutMapping("/roles/{id}/permissions")
    public ApiResponse<RoleResponse> assignPermissions(
            @PathVariable Long id,
            @Valid @RequestBody IdSetRequest request,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:role:assign-permission");
        return ApiResponse.success(rbacService.assignPermissions(id, request.ids()), traceId);
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionResponse>> listPermissions(
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:permission:list");
        return ApiResponse.success(rbacService.listPermissions(), traceId);
    }

    @GetMapping("/users/{id}/roles")
    public ApiResponse<Set<Long>> getUserRoles(
            @PathVariable Long id,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:user:role:list");
        return ApiResponse.success(rbacService.getUserRoleIds(id), traceId);
    }

    @PutMapping("/users/{id}/roles")
    public ApiResponse<Set<Long>> assignUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody IdSetRequest request,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        permissionGuard.require(permissions, "sys:user:assign-role");
        return ApiResponse.success(rbacService.assignUserRoles(id, request.ids()), traceId);
    }
}
