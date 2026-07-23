package com.personaowl.oa.flow.mapper;

import com.personaowl.oa.flow.domain.vo.FlowApproverResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FlowUserDirectoryMapper {
    @Select("""
            SELECT u.display_name
            FROM sys_user u
            WHERE u.id = #{userId} AND u.deleted = 0
            LIMIT 1
            """)
    String findDisplayName(@Param("userId") Long userId);

    @Select("""
            SELECT d.manager_id
            FROM sys_user applicant
            JOIN sys_department d ON d.id = applicant.department_id
            JOIN sys_user manager ON manager.id = d.manager_id
            WHERE applicant.id = #{applicantId}
              AND applicant.deleted = 0
              AND d.deleted = 0
              AND d.status = 1
              AND manager.deleted = 0
              AND manager.status = 1
              AND manager.id <> #{applicantId}
            LIMIT 1
            """)
    Long findDepartmentManager(@Param("applicantId") Long applicantId);

    @Select("""
            SELECT u.id
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE u.status = 1 AND u.deleted = 0
              AND r.status = 1 AND r.deleted = 0
              AND r.code = 'ADMIN'
              AND u.id <> #{applicantId}
            ORDER BY u.id
            LIMIT 1
            """)
    Long findFallbackAdmin(@Param("applicantId") Long applicantId);

    @Select("""
            SELECT u.id
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE u.status = 1 AND u.deleted = 0
              AND r.status = 1 AND r.deleted = 0
              AND r.code = 'MANAGER'
              AND u.id <> #{applicantId}
            ORDER BY u.id
            LIMIT 1
            """)
    Long findFallbackManager(@Param("applicantId") Long applicantId);

    @Select("""
            SELECT u.id, u.username, u.display_name AS displayName,
                   u.department_id AS departmentId, d.name AS departmentName
            FROM sys_user u
            LEFT JOIN sys_department d ON d.id = u.department_id AND d.deleted = 0
            WHERE u.id = #{userId} AND u.status = 1 AND u.deleted = 0
            LIMIT 1
            """)
    FlowApproverResponse findApprover(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT
                u.id,
                u.username,
                u.display_name AS displayName,
                u.department_id AS departmentId,
                d.name AS departmentName
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            LEFT JOIN sys_department d ON d.id = u.department_id AND d.deleted = 0
            WHERE u.status = 1
              AND u.deleted = 0
              AND r.status = 1
              AND r.deleted = 0
              AND r.code IN ('ADMIN', 'MANAGER')
              AND u.id <> #{currentUserId}
            ORDER BY d.sort_order, u.display_name, u.id
            """)
    List<FlowApproverResponse> findAvailableApprovers(@Param("currentUserId") Long currentUserId);
}
