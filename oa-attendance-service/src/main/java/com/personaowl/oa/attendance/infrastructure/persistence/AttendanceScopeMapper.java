package com.personaowl.oa.attendance.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AttendanceScopeMapper {

    @Select("SELECT id FROM sys_department WHERE manager_id = #{userId} AND status = 1 AND deleted = 0 ORDER BY sort_order, id")
    List<Long> findManagedDepartmentIds(@Param("userId") long userId);

    @Select({"<script>",
            "SELECT COUNT(*) FROM sys_user WHERE id = #{userId} AND status = 1 AND deleted = 0",
            "AND department_id IN",
            "<foreach collection='departmentIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"})
    long countUserInDepartments(@Param("userId") long userId,
                                @Param("departmentIds") List<Long> departmentIds);

    @Select("SELECT id, name FROM sys_department WHERE status = 1 AND deleted = 0 ORDER BY sort_order, id")
    List<AttendanceDepartmentEntry> findAllDepartments();

    @Select({"<script>",
            "SELECT id, name FROM sys_department WHERE status = 1 AND deleted = 0 AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "ORDER BY sort_order, id",
            "</script>"})
    List<AttendanceDepartmentEntry> findDepartmentsByIds(@Param("ids") List<Long> ids);

    @Select("""
            SELECT d.id, d.name
            FROM sys_user u
            JOIN sys_department d ON d.id = u.department_id
            WHERE u.id = #{userId} AND u.deleted = 0
              AND d.status = 1 AND d.deleted = 0
            LIMIT 1
            """)
    AttendanceDepartmentEntry findDepartmentByUserId(@Param("userId") long userId);

    @Select("""
            SELECT u.id, u.username, u.display_name, u.department_id, d.name AS department_name
            FROM sys_user u
            LEFT JOIN sys_department d ON d.id = u.department_id AND d.deleted = 0
            WHERE u.status = 1 AND u.deleted = 0
            ORDER BY d.sort_order, u.display_name, u.id
            """)
    List<AttendanceUserDirectoryEntry> findAllUsers();

    @Select({"<script>",
            "SELECT u.id, u.username, u.display_name, u.department_id, d.name AS department_name",
            "FROM sys_user u LEFT JOIN sys_department d ON d.id = u.department_id AND d.deleted = 0",
            "WHERE u.status = 1 AND u.deleted = 0 AND u.department_id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "ORDER BY d.sort_order, u.display_name, u.id",
            "</script>"})
    List<AttendanceUserDirectoryEntry> findUsersByDepartmentIds(@Param("ids") List<Long> ids);

    @Select("""
            SELECT u.id, u.username, u.display_name, u.department_id, d.name AS department_name
            FROM sys_user u
            LEFT JOIN sys_department d ON d.id = u.department_id AND d.deleted = 0
            WHERE u.id = #{userId} AND u.status = 1 AND u.deleted = 0
            LIMIT 1
            """)
    AttendanceUserDirectoryEntry findUserById(@Param("userId") long userId);

    @Select({"<script>",
            "SELECT u.id, u.username, u.display_name, u.department_id, d.name AS department_name",
            "FROM sys_user u LEFT JOIN sys_department d ON d.id = u.department_id AND d.deleted = 0",
            "WHERE u.id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "ORDER BY u.id",
            "</script>"})
    List<AttendanceUserDirectoryEntry> findUsersByIds(@Param("ids") List<Long> ids);
}
