package com.personaowl.oa.user.service;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.user.api.dto.DepartmentCreateRequest;
import com.personaowl.oa.user.api.dto.DepartmentUpdateRequest;
import com.personaowl.oa.user.api.dto.DepartmentResponse;
import com.personaowl.oa.user.domain.SysDepartment;
import com.personaowl.oa.user.mapper.SysDepartmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 部门管理业务层。
 *
 * 负责处理部门查询、创建、修改和删除等业务逻辑。
 */
@Service
public class DepartmentService {

    /**
     * 根部门的父部门 ID。
     */
    private static final long ROOT_DEPARTMENT_ID = 0L;

    private final SysDepartmentMapper departmentMapper;

    public DepartmentService(SysDepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    /**
     * 查询全部未删除部门。
     *
     * @return 部门响应列表
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments() {
        return departmentMapper.findAllAvailable()
            .stream()
            .map(DepartmentResponse::from)
            .toList();
    }

    /**
     * 根据部门 ID 查询部门详情。
     *
     * @param id 部门 ID
     * @return 部门响应对象
     */
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(Long id) {
        SysDepartment department = requireDepartment(id);
        return DepartmentResponse.from(department);
    }

    /**
     * 创建部门。
     *
     * @param request 创建部门请求
     * @return 创建后的部门信息
     */
    @Transactional
    public DepartmentResponse createDepartment(
        DepartmentCreateRequest request
    ) {
        if (request == null) {
            throw new BusinessException(
                ErrorCode.INVALID_ARGUMENT,
                "创建部门请求不能为空"
            );
        }

        Long parentId = normalizeParentId(request.parentId());
        String name = normalizeName(request.name());
        Integer sortOrder = normalizeSortOrder(request.sortOrder());
        Integer status = normalizeStatus(request.status());

        /*
         * 非根部门必须拥有一个真实存在的父部门。
         */
        if (parentId != ROOT_DEPARTMENT_ID) {
            requireDepartment(parentId);
        }

        /*
         * 同一个父部门下面不能有两个名称相同的部门。
         */
        long sameNameCount = departmentMapper.countSameNameForCreate(
            parentId,
            name
        );

        if (sameNameCount > 0) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "同一父部门下已经存在名称为“" + name + "”的部门"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        SysDepartment department = new SysDepartment();
        department.setParentId(parentId);
        department.setName(name);
        department.setSortOrder(sortOrder);
        department.setStatus(status);
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        department.setDeleted(0);

        int affectedRows = departmentMapper.insert(department);

        if (affectedRows != 1) {
            /*
             * 这种情况属于数据库写入异常。
             *
             * 抛出普通运行时异常后，会由 GlobalExceptionHandler
             * 转换成 SYSTEM_ERROR 和 HTTP 500。
             */
            throw new IllegalStateException("创建部门失败");
        }

        return DepartmentResponse.from(department);
    }


    /**
     * 更新部门。
     *
     * @param id      要更新的部门 ID
     * @param request 更新部门请求
     * @return 更新后的部门信息
     */
    @Transactional
    public DepartmentResponse updateDepartment(
        Long id,
        DepartmentUpdateRequest request
    ) {
        if (request == null) {
            throw new BusinessException(
                ErrorCode.INVALID_ARGUMENT,
                "更新部门请求不能为空"
            );
        }

        /*
         * 先确认当前部门存在。
         */
        SysDepartment department = requireDepartment(id);

        Long parentId = normalizeParentId(request.parentId());
        String name = normalizeName(request.name());
        Integer sortOrder = normalizeSortOrder(request.sortOrder());
        Integer status = normalizeStatus(request.status());

        /*
         * 检查新的父部门是否合法。
         *
         * 主要防止：
         * 1. 把自己设置为父部门；
         * 2. 把自己的子部门设置为父部门；
         * 3. 数据库中出现部门循环关系。
         */
        validateParentChain(id, parentId);

        /*
         * 检查同一个父部门下面是否存在其他同名部门。
         *
         * excludeId 参数会排除当前正在修改的部门。
         */
        long sameNameCount = departmentMapper.countSameNameForUpdate(
            parentId,
            name,
            id
        );

        if (sameNameCount > 0) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "同一父部门下已经存在名称为“" + name + "”的部门"
            );
        }

        department.setParentId(parentId);
        department.setName(name);
        department.setSortOrder(sortOrder);
        department.setStatus(status);
        department.setUpdatedAt(LocalDateTime.now());

        int affectedRows = departmentMapper.updateById(department);

        if (affectedRows != 1) {
            throw new IllegalStateException("更新部门失败");
        }

        return DepartmentResponse.from(department);
    }


    /**
     * 删除部门。
     *
     * 删除前需要确认：
     * 1. 部门存在；
     * 2. 部门下没有子部门；
     * 3. 部门下没有员工。
     *
     * @param id 要删除的部门 ID
     */
    @Transactional
    public void deleteDepartment(Long id) {
        /*
         * 先检查部门 ID 是否合法，并确认部门存在。
         */
        requireDepartment(id);

        /*
         * 当前部门下存在子部门时不能删除，
         * 否则子部门会失去父部门。
         */
        long childCount = departmentMapper.countChildren(id);

        if (childCount > 0) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "当前部门下存在子部门，不能删除"
            );
        }

        /*
         * 当前部门下存在员工时不能删除，
         * 需要先转移或删除这些员工。
         */
        long userCount = departmentMapper.countUsers(id);

        if (userCount > 0) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "当前部门下存在员工，不能删除"
            );
        }

        /*
         * 执行逻辑删除。
         *
         * Mapper 中的 softDelete() 会把 deleted 修改为 1，
         * 并修改原部门名称，释放同级名称唯一索引。
         */
        int affectedRows = departmentMapper.softDelete(id);

        if (affectedRows != 1) {
            throw new IllegalStateException("删除部门失败");
        }
    }

    /**
     * 查询一个必须存在的部门。
     *
     * @param id 部门 ID
     * @return 部门实体
     */
    private SysDepartment requireDepartment(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                ErrorCode.INVALID_ARGUMENT,
                "部门ID必须是正整数"
            );
        }

        SysDepartment department =
            departmentMapper.findAvailableById(id);

        if (department == null) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "部门不存在或已被删除"
            );
        }

        return department;
    }


    /**
     * 检查新的父部门关系是否合法。
     *
     * @param currentDepartmentId 当前正在修改的部门 ID
     * @param parentId            新的父部门 ID
     */
    private void validateParentChain(
        Long currentDepartmentId,
        Long parentId
    ) {
        /*
         * 父部门为 0，表示设置为根部门，不需要继续检查。
         */
        if (parentId == ROOT_DEPARTMENT_ID) {
            return;
        }

        /*
         * 防止数据库中原本就存在循环关系时一直查询下去。
         */
        Set<Long> visited = new HashSet<>();

        Long currentParentId = parentId;

        while (currentParentId != null
            && currentParentId != ROOT_DEPARTMENT_ID) {

            /*
             * 在向上查找父部门的过程中遇到当前部门，
             * 说明会产生循环关系。
             */
            if (currentDepartmentId.equals(currentParentId)) {
                throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "不能将当前部门或其子部门设置为父部门"
                );
            }

            /*
             * add() 返回 false，表示该 ID 之前已经出现过，
             * 说明数据库中已经存在循环关系。
             */
            if (!visited.add(currentParentId)) {
                throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "检测到非法的部门循环关系"
                );
            }

            SysDepartment parentDepartment =
                departmentMapper.findAvailableById(currentParentId);

            if (parentDepartment == null) {
                throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "指定的父部门不存在或已被删除"
                );
            }

            currentParentId = normalizeParentId(
                parentDepartment.getParentId()
            );
        }
    }
    /**
     * 处理父部门 ID。
     *
     * null 会被转换成 0，表示根部门。
     */
    private Long normalizeParentId(Long parentId) {
        if (parentId == null) {
            return ROOT_DEPARTMENT_ID;
        }

        if (parentId < 0) {
            throw new BusinessException(
                ErrorCode.INVALID_ARGUMENT,
                "父部门ID不能小于0"
            );
        }

        return parentId;
    }

    /**
     * 处理部门名称。
     */
    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(
                ErrorCode.INVALID_ARGUMENT,
                "部门名称不能为空"
            );
        }

        String normalizedName = name.trim();

        if (normalizedName.length() > 64) {
            throw new BusinessException(
                ErrorCode.INVALID_ARGUMENT,
                "部门名称长度不能超过64个字符"
            );
        }

        return normalizedName;
    }

    /**
     * 处理排序号。
     *
     * null 默认转换成 0。
     */
    private Integer normalizeSortOrder(Integer sortOrder) {
        if (sortOrder == null) {
            return 0;
        }

        if (sortOrder < 0) {
            throw new BusinessException(
                ErrorCode.INVALID_ARGUMENT,
                "排序号不能小于0"
            );
        }

        return sortOrder;
    }

    /**
     * 处理部门状态。
     *
     * null 默认转换成 1，也就是启用状态。
     */
    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }

        if (status != 0 && status != 1) {
            throw new BusinessException(
                ErrorCode.INVALID_ARGUMENT,
                "部门状态只能为0或1"
            );
        }

        return status;
    }
}
