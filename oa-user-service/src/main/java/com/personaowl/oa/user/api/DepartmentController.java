package com.personaowl.oa.user.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.user.api.dto.DepartmentCreateRequest;
import com.personaowl.oa.user.api.dto.DepartmentResponse;
import com.personaowl.oa.user.api.dto.DepartmentUpdateRequest;
import com.personaowl.oa.user.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门管理接口。
 */
@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 通过构造方法注入部门业务层。
     *
     * @param departmentService 部门业务层
     */
    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * 查询全部部门。
     *
     * 请求方式：
     * GET /api/v1/departments
     *
     * @param traceId 链路追踪 ID
     * @return 部门列表
     */
    @GetMapping
    public ApiResponse<List<DepartmentResponse>> listDepartments(
        @RequestHeader(
            value = RequestHeaders.TRACE_ID,
            required = false
        ) String traceId
    ) {
        List<DepartmentResponse> departments =
            departmentService.listDepartments();

        return ApiResponse.success(departments, traceId);
    }

    /**
     * 根据部门 ID 查询部门详情。
     *
     * 请求方式：
     * GET /api/v1/departments/{id}
     *
     * @param id      部门 ID
     * @param traceId 链路追踪 ID
     * @return 部门详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DepartmentResponse> getDepartment(
        @PathVariable Long id,
        @RequestHeader(
            value = RequestHeaders.TRACE_ID,
            required = false
        ) String traceId
    ) {
        DepartmentResponse department =
            departmentService.getDepartment(id);

        return ApiResponse.success(department, traceId);
    }

    /**
     * 创建部门。
     *
     * 请求方式：
     * POST /api/v1/departments
     *
     * @param request 创建部门请求
     * @param traceId 链路追踪 ID
     * @return 创建后的部门信息
     */
    @PostMapping
    public ApiResponse<DepartmentResponse> createDepartment(
        @Valid @RequestBody DepartmentCreateRequest request,
        @RequestHeader(
            value = RequestHeaders.TRACE_ID,
            required = false
        ) String traceId
    ) {
        DepartmentResponse department =
            departmentService.createDepartment(request);

        return ApiResponse.success(department, traceId);
    }

    /**
     * 更新部门。
     *
     * 请求方式：
     * PUT /api/v1/departments/{id}
     *
     * @param id      部门 ID
     * @param request 更新部门请求
     * @param traceId 链路追踪 ID
     * @return 更新后的部门信息
     */
    @PutMapping("/{id}")
    public ApiResponse<DepartmentResponse> updateDepartment(
        @PathVariable Long id,
        @Valid @RequestBody DepartmentUpdateRequest request,
        @RequestHeader(
            value = RequestHeaders.TRACE_ID,
            required = false
        ) String traceId
    ) {
        DepartmentResponse department =
            departmentService.updateDepartment(id, request);

        return ApiResponse.success(department, traceId);
    }

    /**
     * 删除部门。
     *
     * 请求方式：
     * DELETE /api/v1/departments/{id}
     *
     * @param id      部门 ID
     * @param traceId 链路追踪 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDepartment(
        @PathVariable Long id,
        @RequestHeader(
            value = RequestHeaders.TRACE_ID,
            required = false
        ) String traceId
    ) {
        departmentService.deleteDepartment(id);

        return ApiResponse.success(null, traceId);
    }
}
