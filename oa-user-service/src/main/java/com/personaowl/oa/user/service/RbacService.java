package com.personaowl.oa.user.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.user.api.dto.PermissionResponse;
import com.personaowl.oa.user.api.dto.RoleCreateRequest;
import com.personaowl.oa.user.api.dto.RoleResponse;
import com.personaowl.oa.user.api.dto.RoleUpdateRequest;
import com.personaowl.oa.user.domain.SysRole;
import com.personaowl.oa.user.mapper.SysPermissionMapper;
import com.personaowl.oa.user.mapper.SysRoleMapper;
import com.personaowl.oa.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RbacService {
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysUserMapper userMapper;

    public RbacService(SysRoleMapper roleMapper, SysPermissionMapper permissionMapper, SysUserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleMapper.findAllAvailable().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(Long roleId) {
        return toResponse(requireRole(roleId));
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        String code = normalizeCode(request.code());
        ensureUniqueCode(code, 0L);
        LocalDateTime now = LocalDateTime.now();
        SysRole role = new SysRole();
        role.setId(IdWorker.getId());
        role.setCode(code);
        role.setName(request.name().trim());
        role.setStatus(request.status() == null ? 1 : request.status());
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        role.setDeleted(0);
        if (roleMapper.insert(role) != 1) {
            throw new IllegalStateException("创建角色失败");
        }
        return RoleResponse.from(role, Set.of());
    }

    @Transactional
    public RoleResponse updateRole(Long roleId, RoleUpdateRequest request) {
        SysRole role = requireRole(roleId);
        String code = normalizeCode(request.code());
        ensureUniqueCode(code, roleId);
        role.setCode(code);
        role.setName(request.name().trim());
        role.setStatus(request.status() == null ? 1 : request.status());
        role.setUpdatedAt(LocalDateTime.now());
        if (roleMapper.updateById(role) != 1) {
            throw new IllegalStateException("更新角色失败");
        }
        return toResponse(role);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        requireRole(roleId);
        if (roleMapper.countAssignedUsers(roleId) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "角色仍被用户使用，不能删除");
        }
        roleMapper.deleteRolePermissions(roleId);
        if (roleMapper.softDelete(roleId) != 1) {
            throw new IllegalStateException("删除角色失败");
        }
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionMapper.findAllAvailable().stream().map(PermissionResponse::from).toList();
    }

    @Transactional
    public RoleResponse assignPermissions(Long roleId, Set<Long> requestedIds) {
        SysRole role = requireRole(roleId);
        Set<Long> permissionIds = normalizeIds(requestedIds, "权限ID");
        if (!permissionIds.isEmpty() && permissionMapper.countAvailableIds(permissionIds) != permissionIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "包含不存在或已删除的权限");
        }
        roleMapper.deleteRolePermissions(roleId);
        permissionIds.forEach(permissionId -> roleMapper.insertRolePermission(roleId, permissionId));
        return RoleResponse.from(role, permissionIds);
    }

    @Transactional(readOnly = true)
    public Set<Long> getUserRoleIds(Long userId) {
        requireUser(userId);
        return new LinkedHashSet<>(userMapper.findRoleIds(userId));
    }

    @Transactional
    public Set<Long> assignUserRoles(Long userId, Set<Long> requestedIds) {
        requireUser(userId);
        Set<Long> roleIds = normalizeIds(requestedIds, "角色ID");
        if (!roleIds.isEmpty() && roleMapper.countEnabledIds(roleIds) != roleIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "包含不存在、已停用或已删除的角色");
        }
        userMapper.deleteUserRoles(userId);
        roleIds.forEach(roleId -> userMapper.insertUserRole(userId, roleId));
        return roleIds;
    }

    private RoleResponse toResponse(SysRole role) {
        return RoleResponse.from(role, new LinkedHashSet<>(roleMapper.findPermissionIds(role.getId())));
    }

    private SysRole requireRole(Long roleId) {
        if (roleId == null || roleId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "角色ID必须是正整数");
        }
        SysRole role = roleMapper.findAvailableById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "角色不存在或已删除");
        }
        return role;
    }

    private void requireUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "用户ID必须是正整数");
        }
        if (userMapper.findAvailableById(userId) == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "用户不存在或已删除");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void ensureUniqueCode(String code, Long excludeId) {
        if (roleMapper.countCodeExcluding(code, excludeId) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "角色编码已存在");
        }
    }

    private Set<Long> normalizeIds(Set<Long> ids, String label) {
        if (ids == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "集合不能为空");
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "必须是正整数");
        }
        return new LinkedHashSet<>(ids);
    }
}
