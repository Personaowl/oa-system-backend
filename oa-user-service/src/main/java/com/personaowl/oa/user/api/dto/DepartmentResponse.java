package com.personaowl.oa.user.api.dto;

import com.personaowl.oa.user.domain.SysDepartment;

import java.time.LocalDateTime;

/**
 * 部门接口响应对象。
 *
 * 用于向前端返回部门信息。
 */
public record DepartmentResponse(

    /**
     * 部门主键。
     */
    Long id,

    /**
     * 父部门 ID。
     *
     * 0 表示根部门。
     */
    Long parentId,

    /**
     * 部门名称。
     */
    String name,

    /**
     * 排序号。
     */
    Integer sortOrder,

    /**
     * 部门状态：
     * 1 表示启用；
     * 0 表示禁用。
     */
    Integer status,

    /**
     * 创建时间。
     */
    LocalDateTime createdAt,

    /**
     * 最后更新时间。
     */
    LocalDateTime updatedAt
) {

    /**
     * 将部门数据库实体转换成接口响应对象。
     *
     * @param department 部门实体
     * @return 部门响应对象
     */
    public static DepartmentResponse from(SysDepartment department) {
        return new DepartmentResponse(
            department.getId(),
            department.getParentId(),
            department.getName(),
            department.getSortOrder(),
            department.getStatus(),
            department.getCreatedAt(),
            department.getUpdatedAt()
        );
    }
}
