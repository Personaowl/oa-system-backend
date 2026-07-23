package com.personaowl.oa.document.infrastructure;

import com.personaowl.oa.document.domain.DocumentUserScope;
import com.personaowl.oa.document.domain.DocumentWorkspace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentAccessMapper {

    @Select("""
        SELECT id AS userId, department_id AS departmentId, display_name AS displayName
        FROM sys_user
        WHERE id = #{userId} AND deleted = 0 AND status = 1
        LIMIT 1
        """)
    DocumentUserScope findUserScope(@Param("userId") Long userId);

    @Select("""
        SELECT DISTINCT department_id
        FROM sys_department_manager
        WHERE user_id = #{userId}
        UNION
        SELECT id
        FROM sys_department
        WHERE manager_id = #{userId} AND deleted = 0
        """)
    List<Long> findManagedDepartmentIds(@Param("userId") Long userId);

    @Select("SELECT id FROM sys_department WHERE deleted = 0 AND status = 1 ORDER BY sort_order, id")
    List<Long> findAllDepartmentIds();

    @Select("""
        <script>
        SELECT d.id AS departmentId,
               d.name AS departmentName,
               COUNT(DISTINCT u.id) AS memberCount,
               COUNT(DISTINCT sd.id) AS documentCount
        FROM sys_department d
        LEFT JOIN sys_user u
               ON u.department_id = d.id AND u.deleted = 0 AND u.status = 1
        LEFT JOIN shared_document sd
               ON sd.department_id = d.id AND sd.deleted = 0
        WHERE d.deleted = 0 AND d.status = 1
          AND d.id IN
          <foreach collection="departmentIds" item="departmentId" open="(" separator="," close=")">
            #{departmentId}
          </foreach>
        GROUP BY d.id, d.name, d.sort_order
        ORDER BY d.sort_order, d.id
        </script>
        """)
    List<DocumentWorkspace> findWorkspaces(@Param("departmentIds") List<Long> departmentIds);
}
