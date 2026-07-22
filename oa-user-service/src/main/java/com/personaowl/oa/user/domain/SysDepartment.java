package com.personaowl.oa.user.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 部门实体类。
 *
 * 对应数据库表：sys_department
 */
@TableName("sys_department")
public class SysDepartment {

    /**
     * 部门主键。
     */
    @TableId
    private Long id;

    /**
     * 父部门 ID。
     *
     * 0 表示当前部门为根部门。
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 部门名称。
     */
    private String name;

    /**
     * 部门负责人用户 ID。负责人属于组织关系，不与 RBAC 角色强绑定。
     */
    @TableField("manager_id")
    private Long managerId;

    /**
     * 排序号，数值越小越靠前。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态：
     * 1 表示启用；
     * 0 表示禁用。
     */
    private Integer status;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 最后更新时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记：
     * 0 表示未删除；
     * 1 表示已删除。
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
