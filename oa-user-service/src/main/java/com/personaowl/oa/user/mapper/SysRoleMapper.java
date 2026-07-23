package com.personaowl.oa.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.user.domain.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Set;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    @Select("SELECT id, code, name, status, created_at, updated_at, deleted FROM sys_role WHERE deleted = 0 ORDER BY id")
    List<SysRole> findAllAvailable();

    @Select("SELECT id, code, name, status, created_at, updated_at, deleted FROM sys_role WHERE id = #{id} AND deleted = 0")
    SysRole findAvailableById(@Param("id") Long id);

    @Select("<script>SELECT COUNT(*) FROM sys_role WHERE deleted = 0 AND status = 1 AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    long countEnabledIds(@Param("ids") Set<Long> ids);

    @Select("SELECT COUNT(*) FROM sys_role WHERE code = #{code} AND deleted = 0 AND id <> #{excludeId}")
    long countCodeExcluding(@Param("code") String code, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    long countAssignedUsers(@Param("roleId") Long roleId);

    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId} ORDER BY permission_id")
    List<Long> findPermissionIds(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteRolePermissions(@Param("roleId") Long roleId);

    @Insert("INSERT INTO sys_role_permission(role_id, permission_id) VALUES(#{roleId}, #{permissionId})")
    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    @Update("UPDATE sys_role SET code = CONCAT('DELETED_', id), deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted = 0")
    int softDelete(@Param("id") Long id);
}
