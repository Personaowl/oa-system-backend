package com.personaowl.oa.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.user.domain.SysDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 部门数据访问接口。
 *
 * 负责访问数据库中的 sys_department 表。
 */
@Mapper
public interface SysDepartmentMapper extends BaseMapper<SysDepartment> {

    /**
     * 查询全部未删除的部门。
     *
     * 先按排序号升序排列，排序号相同时再按主键排列。
     */
    @Select("""
            SELECT id,
                   parent_id,
                   name,
                   sort_order,
                   status,
                   created_at,
                   updated_at,
                   deleted
            FROM sys_department
            WHERE deleted = 0
            ORDER BY sort_order ASC, id ASC
            """)
    List<SysDepartment> findAllAvailable();

    /**
     * 根据部门 ID 查询一个未删除的部门。
     */
    @Select("""
            SELECT id,
                   parent_id,
                   name,
                   sort_order,
                   status,
                   created_at,
                   updated_at,
                   deleted
            FROM sys_department
            WHERE id = #{id}
              AND deleted = 0
            LIMIT 1
            """)
    SysDepartment findAvailableById(@Param("id") Long id);

    /**
     * 创建部门时，检查同一父部门下是否存在同名部门。
     */
    @Select("""
            SELECT COUNT(*)
            FROM sys_department
            WHERE parent_id = #{parentId}
              AND name = #{name}
              AND deleted = 0
            """)
    long countSameNameForCreate(
        @Param("parentId") Long parentId,
        @Param("name") String name
    );

    /**
     * 更新部门时，检查同一父部门下是否存在其他同名部门。
     *
     * 当前正在更新的部门自身需要排除。
     */
    @Select("""
            SELECT COUNT(*)
            FROM sys_department
            WHERE parent_id = #{parentId}
              AND name = #{name}
              AND id <> #{excludeId}
              AND deleted = 0
            """)
    long countSameNameForUpdate(
        @Param("parentId") Long parentId,
        @Param("name") String name,
        @Param("excludeId") Long excludeId
    );

    /**
     * 查询某个部门的直接子部门数量。
     */
    @Select("""
            SELECT COUNT(*)
            FROM sys_department
            WHERE parent_id = #{parentId}
              AND deleted = 0
            """)
    long countChildren(@Param("parentId") Long parentId);

    /**
     * 查询某个部门下未删除的员工数量。
     */
    @Select("""
            SELECT COUNT(*)
            FROM sys_user
            WHERE department_id = #{departmentId}
              AND deleted = 0
            """)
    long countUsers(@Param("departmentId") Long departmentId);

    /**
     * 逻辑删除部门。
     *
     * 删除时同时修改名称，是为了释放数据库中的同级部门名称唯一索引。
     */
    @Update("""
            UPDATE sys_department
            SET name = CONCAT('deleted_', id),
                deleted = 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
            """)
    int softDelete(@Param("id") Long id);
}
