package com.personaowl.oa.user.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 更新部门请求参数。
 *
 * 前端修改部门信息时，需要提交本对象中的字段。
 */
public record DepartmentUpdateRequest(

    /**
     * 父部门 ID。
     *
     * 0 或 null 表示根部门。
     */
    @Min(value = 0, message = "父部门ID不能小于0")
    Long parentId,

    /**
     * 部门名称。
     */
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 64, message = "部门名称长度不能超过64个字符")
    String name,

    /** 可选的部门负责人用户 ID。 */
    @Min(value = 1, message = "负责人ID必须是正整数")
    Long managerId,

    /** 可选的协同主管用户 ID，主负责人无需重复传入。 */
    @Size(max = 10, message = "一个部门最多配置10名协同主管")
    List<@Min(value = 1, message = "协同主管ID必须是正整数") Long> assistantManagerIds,

    /**
     * 排序号，数值越小越靠前。
     */
    @Min(value = 0, message = "排序号不能小于0")
    Integer sortOrder,

    /**
     * 部门状态：
     * 1 表示启用；
     * 0 表示禁用。
     */
    @Min(value = 0, message = "部门状态只能为0或1")
    @Max(value = 1, message = "部门状态只能为0或1")
    Integer status
) {
    public DepartmentUpdateRequest(Long parentId, String name, Integer sortOrder, Integer status) {
        this(parentId, name, null, List.of(), sortOrder, status);
    }

    public DepartmentUpdateRequest(
            Long parentId, String name, Long managerId, Integer sortOrder, Integer status) {
        this(parentId, name, managerId, List.of(), sortOrder, status);
    }
}
