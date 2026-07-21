package com.personaowl.oa.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.user.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT id, department_id, username, password_hash, display_name, phone, email, status, deleted
            FROM sys_user
            WHERE username = #{username} AND status = 1 AND deleted = 0
            LIMIT 1
            """)
    SysUser findEnabledByUsername(@Param("username") String username);

    @Select("SELECT COUNT(1) FROM sys_user WHERE username = #{username}")
    long countByUsername(@Param("username") String username);

    @Select("""
            SELECT id, department_id, username, password_hash, display_name, phone, email, status, deleted
            FROM sys_user
            WHERE id = #{userId} AND status = 1 AND deleted = 0
            LIMIT 1
            """)
    SysUser findEnabledById(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT r.code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0
            ORDER BY r.code
            """)
    List<String> findRoleCodes(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT p.code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.status = 1 AND r.deleted = 0
              AND p.deleted = 0
            ORDER BY p.code
            """)
    List<String> findPermissionCodes(@Param("userId") Long userId);
}
