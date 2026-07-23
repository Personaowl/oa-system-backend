package com.personaowl.oa.user.service;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.user.domain.SysDepartment;
import com.personaowl.oa.user.mapper.SysDepartmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.personaowl.oa.user.api.dto.DepartmentCreateRequest;
import com.personaowl.oa.user.api.dto.DepartmentUpdateRequest;
import com.personaowl.oa.user.api.dto.DepartmentCreateRequest;
import com.personaowl.oa.user.api.dto.DepartmentUpdateRequest;
import com.personaowl.oa.user.domain.SysDepartment;

import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

/**
 * 部门业务层单元测试。
 *
 * 本测试使用 Mockito 模拟 Mapper，
 * 不连接真实数据库，也不启动 Spring Boot。
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    /**
     * 模拟部门 Mapper。
     */
    @Mock
    private SysDepartmentMapper departmentMapper;

    /**
     * 被测试的部门 Service。
     */
    private DepartmentService departmentService;

    /**
     * 每个测试执行之前，创建一个新的 Service。
     */
    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentMapper);
    }

    /**
     * 查询全部部门时，应当把实体列表转换成响应列表。
     */
    @Test
    void listDepartmentsReturnsDepartmentResponses() {
        SysDepartment headquarters = department(
            1L,
            0L,
            "总部",
            0,
            1
        );

        SysDepartment development = department(
            2L,
            1L,
            "研发部",
            10,
            1
        );

        when(departmentMapper.findAllAvailable())
            .thenReturn(List.of(headquarters, development));

        var result = departmentService.listDepartments();

        assertThat(result).hasSize(2);

        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).parentId()).isEqualTo(0L);
        assertThat(result.get(0).name()).isEqualTo("总部");
        assertThat(result.get(0).sortOrder()).isEqualTo(0);
        assertThat(result.get(0).status()).isEqualTo(1);

        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).parentId()).isEqualTo(1L);
        assertThat(result.get(1).name()).isEqualTo("研发部");
        assertThat(result.get(1).sortOrder()).isEqualTo(10);
        assertThat(result.get(1).status()).isEqualTo(1);

        verify(departmentMapper).findAllAvailable();
    }

    /**
     * 根据合法 ID 查询部门时，应返回对应部门。
     */
    @Test
    void getDepartmentReturnsDepartmentWhenDepartmentExists() {
        SysDepartment department = department(
            2L,
            1L,
            "研发部",
            10,
            1
        );

        when(departmentMapper.findAvailableById(2L))
            .thenReturn(department);

        var result = departmentService.getDepartment(2L);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.parentId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("研发部");
        assertThat(result.sortOrder()).isEqualTo(10);
        assertThat(result.status()).isEqualTo(1);

        verify(departmentMapper).findAvailableById(2L);
    }

    /**
     * 部门 ID 为 0 时，应抛出参数异常。
     */
    @Test
    void getDepartmentRejectsInvalidId() {
        assertThatThrownBy(() ->
            departmentService.getDepartment(0L)
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.errorCode())
                    .isEqualTo(ErrorCode.INVALID_ARGUMENT);

                assertThat(exception.getMessage())
                    .isEqualTo("部门ID必须是正整数");
            }
        );
    }

    /**
     * Mapper 查询不到部门时，应抛出业务规则异常。
     */
    @Test
    void getDepartmentRejectsMissingDepartment() {
        when(departmentMapper.findAvailableById(99L))
            .thenReturn(null);

        assertThatThrownBy(() ->
            departmentService.getDepartment(99L)
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.errorCode())
                    .isEqualTo(
                        ErrorCode.BUSINESS_RULE_VIOLATION
                    );

                assertThat(exception.getMessage())
                    .isEqualTo("部门不存在或已被删除");
            }
        );

        verify(departmentMapper).findAvailableById(99L);
    }


    /**
     * 创建根部门时，应处理默认值、去除名称两侧空格，
     * 并将部门保存到数据库。
     */
    @Test
    void createDepartmentCreatesRootDepartment() {
        DepartmentCreateRequest request =
            new DepartmentCreateRequest(
                null,
                "  行政部  ",
                null,
                null
            );

        when(departmentMapper.countSameNameForCreate(
            0L,
            "行政部"
        )).thenReturn(0L);

        when(departmentMapper.insert(any(SysDepartment.class)))
            .thenAnswer(invocation -> {
                SysDepartment department =
                    invocation.getArgument(0);

                /*
                 * 模拟数据库插入完成后，
                 * MyBatis-Plus 将生成的主键写回实体。
                 */
                department.setId(3L);

                return 1;
            });

        var result =
            departmentService.createDepartment(request);

        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.parentId()).isEqualTo(0L);
        assertThat(result.name()).isEqualTo("行政部");
        assertThat(result.sortOrder()).isEqualTo(0);
        assertThat(result.status()).isEqualTo(1);
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.updatedAt()).isNotNull();

        ArgumentCaptor<SysDepartment> captor =
            ArgumentCaptor.forClass(SysDepartment.class);

        verify(departmentMapper).insert(captor.capture());

        SysDepartment insertedDepartment =
            captor.getValue();

        assertThat(insertedDepartment.getParentId())
            .isEqualTo(0L);
        assertThat(insertedDepartment.getName())
            .isEqualTo("行政部");
        assertThat(insertedDepartment.getSortOrder())
            .isEqualTo(0);
        assertThat(insertedDepartment.getStatus())
            .isEqualTo(1);
        assertThat(insertedDepartment.getDeleted())
            .isEqualTo(0);

        verify(departmentMapper)
            .countSameNameForCreate(0L, "行政部");
    }


    /**
     * 创建子部门时，应先确认父部门存在。
     */
    @Test
    void createDepartmentCreatesChildDepartmentWhenParentExists() {
        SysDepartment parentDepartment = department(
            1L,
            0L,
            "总部",
            0,
            1
        );

        DepartmentCreateRequest request =
            new DepartmentCreateRequest(
                1L,
                "研发部",
                10,
                1
            );

        /*
         * 模拟父部门存在。
         */
        when(departmentMapper.findAvailableById(1L))
            .thenReturn(parentDepartment);

        /*
         * 模拟同一父部门下没有重名部门。
         */
        when(departmentMapper.countSameNameForCreate(
            1L,
            "研发部"
        )).thenReturn(0L);

        /*
         * 模拟数据库插入成功，并回填部门 ID。
         */
        when(departmentMapper.insert(any(SysDepartment.class)))
            .thenAnswer(invocation -> {
                SysDepartment insertedDepartment =
                    invocation.getArgument(0);

                insertedDepartment.setId(2L);

                return 1;
            });

        var result =
            departmentService.createDepartment(request);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.parentId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("研发部");
        assertThat(result.sortOrder()).isEqualTo(10);
        assertThat(result.status()).isEqualTo(1);
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.updatedAt()).isNotNull();

        /*
         * 验证创建子部门前确实检查了父部门。
         */
        verify(departmentMapper).findAvailableById(1L);

        /*
         * 验证检查了同级名称是否重复。
         */
        verify(departmentMapper)
            .countSameNameForCreate(1L, "研发部");

        /*
         * 验证最终执行了插入。
         */
        verify(departmentMapper)
            .insert(any(SysDepartment.class));
    }
    /**
     * 同一个父部门下存在同名部门时，应拒绝创建。
     */
    @Test
    void createDepartmentRejectsDuplicateName() {
        DepartmentCreateRequest request =
            new DepartmentCreateRequest(
                0L,
                "行政部",
                0,
                1
            );

        when(departmentMapper.countSameNameForCreate(
            0L,
            "行政部"
        )).thenReturn(1L);

        assertThatThrownBy(() ->
            departmentService.createDepartment(request)
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.errorCode())
                    .isEqualTo(
                        ErrorCode.BUSINESS_RULE_VIOLATION
                    );

                assertThat(exception.getMessage())
                    .contains("已经存在")
                    .contains("行政部");
            }
        );

        verify(departmentMapper)
            .countSameNameForCreate(0L, "行政部");

        verify(departmentMapper, never())
            .insert(any(SysDepartment.class));
    }

    /**
     * 创建子部门时，指定的父部门不存在，应拒绝创建。
     */
    @Test
    void createDepartmentRejectsMissingParent() {
        DepartmentCreateRequest request =
            new DepartmentCreateRequest(
                99L,
                "测试部",
                0,
                1
            );

        when(departmentMapper.findAvailableById(99L))
            .thenReturn(null);

        assertThatThrownBy(() ->
            departmentService.createDepartment(request)
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.errorCode())
                    .isEqualTo(
                        ErrorCode.BUSINESS_RULE_VIOLATION
                    );

                assertThat(exception.getMessage())
                    .isEqualTo("部门不存在或已被删除");
            }
        );

        verify(departmentMapper)
            .findAvailableById(99L);

        verify(departmentMapper, never())
            .countSameNameForCreate(
                any(Long.class),
                any(String.class)
            );

        verify(departmentMapper, never())
            .insert(any(SysDepartment.class));
    }

    /**
     * 更新部门时，应保存新的部门信息并返回更新结果。
     */
    @Test
    void updateDepartmentUpdatesExistingDepartment() {
        /*
         * 模拟数据库中已经存在研发部。
         */
        SysDepartment existingDepartment = department(
            2L,
            1L,
            "研发部",
            10,
            1
        );

        /*
         * 本次把研发部修改为技术研发部，
         * 并移动为根部门。
         */
        DepartmentUpdateRequest request =
            new DepartmentUpdateRequest(
                0L,
                " 技术研发部 ",
                20,
                0
            );

        /*
         * Service 首先会查询当前要修改的部门。
         */
        when(departmentMapper.findAvailableById(2L))
            .thenReturn(existingDepartment);

        /*
         * 模拟根部门下不存在其他同名部门。
         */
        when(departmentMapper.countSameNameForUpdate(
            0L,
            "技术研发部",
            2L
        )).thenReturn(0L);

        /*
         * 模拟数据库更新成功。
         */
        when(departmentMapper.updateById(
            any(SysDepartment.class)
        )).thenReturn(1);

        var result = departmentService.updateDepartment(
            2L,
            request
        );

        /*
         * 检查返回结果。
         */
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.parentId()).isEqualTo(0L);
        assertThat(result.name()).isEqualTo("技术研发部");
        assertThat(result.sortOrder()).isEqualTo(20);
        assertThat(result.status()).isEqualTo(0);
        assertThat(result.updatedAt()).isNotNull();

        /*
         * 捕获实际传给 Mapper 的实体，
         * 检查 Service 是否正确修改了字段。
         */
        ArgumentCaptor<SysDepartment> captor =
            ArgumentCaptor.forClass(SysDepartment.class);

        verify(departmentMapper).updateById(captor.capture());

        SysDepartment updatedDepartment = captor.getValue();

        assertThat(updatedDepartment.getId()).isEqualTo(2L);
        assertThat(updatedDepartment.getParentId()).isEqualTo(0L);
        assertThat(updatedDepartment.getName())
            .isEqualTo("技术研发部");
        assertThat(updatedDepartment.getSortOrder())
            .isEqualTo(20);
        assertThat(updatedDepartment.getStatus())
            .isEqualTo(0);

        /*
         * 验证查询和重名检查确实执行过。
         */
        verify(departmentMapper).findAvailableById(2L);

        verify(departmentMapper).countSameNameForUpdate(
            0L,
            "技术研发部",
            2L
        );
    }
    /**
     * 更新部门时，不能将部门自己设置为自己的父部门。
     */
    @Test
    void updateDepartmentRejectsSelfAsParent() {
        SysDepartment existingDepartment = department(
            2L,
            1L,
            "研发部",
            10,
            1
        );

        DepartmentUpdateRequest request =
            new DepartmentUpdateRequest(
                2L,
                "研发部",
                10,
                1
            );

        /*
         * updateDepartment() 会先查询当前部门是否存在。
         */
        when(departmentMapper.findAvailableById(2L))
            .thenReturn(existingDepartment);

        assertThatThrownBy(() ->
            departmentService.updateDepartment(
                2L,
                request
            )
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.errorCode())
                    .isEqualTo(
                        ErrorCode.BUSINESS_RULE_VIOLATION
                    );

                assertThat(exception.getMessage())
                    .isEqualTo(
                        "不能将当前部门或其子部门设置为父部门"
                    );
            }
        );

        /*
         * 当前部门应该被查询一次。
         */
        verify(departmentMapper)
            .findAvailableById(2L);

        /*
         * 检测到父部门就是自己后，
         * 不应该继续检查重名，也不应该更新数据库。
         */
        verify(departmentMapper, never())
            .countSameNameForUpdate(
                any(Long.class),
                any(String.class),
                any(Long.class)
            );

        verify(departmentMapper, never())
            .updateById(any(SysDepartment.class));
    }
    /**
     * 更新部门时，同一父部门下存在其他同名部门，应拒绝更新。
     */
    @Test
    void updateDepartmentRejectsDuplicateName() {
        SysDepartment existingDepartment = department(
            2L,
            1L,
            "研发部",
            10,
            1
        );

        DepartmentUpdateRequest request =
            new DepartmentUpdateRequest(
                1L,
                "技术部",
                20,
                1
            );

        /*
         * 当前要修改的部门存在。
         */
        when(departmentMapper.findAvailableById(2L))
            .thenReturn(existingDepartment);

        /*
         * validateParentChain() 会查询父部门 1。
         */
        SysDepartment parentDepartment = department(
            1L,
            0L,
            "总部",
            0,
            1
        );

        when(departmentMapper.findAvailableById(1L))
            .thenReturn(parentDepartment);

        /*
         * 模拟同一个父部门下已经存在其他“技术部”。
         */
        when(departmentMapper.countSameNameForUpdate(
            1L,
            "技术部",
            2L
        )).thenReturn(1L);

        assertThatThrownBy(() ->
            departmentService.updateDepartment(
                2L,
                request
            )
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.errorCode())
                    .isEqualTo(
                        ErrorCode.BUSINESS_RULE_VIOLATION
                    );

                assertThat(exception.getMessage())
                    .contains("已经存在")
                    .contains("技术部");
            }
        );

        verify(departmentMapper).findAvailableById(2L);
        verify(departmentMapper).findAvailableById(1L);

        verify(departmentMapper).countSameNameForUpdate(
            1L,
            "技术部",
            2L
        );

        /*
         * 名称重复时，不允许写入数据库。
         */
        verify(departmentMapper, never())
            .updateById(any(SysDepartment.class));
    }

    /**
     * 部门下存在子部门时，应拒绝删除。
     */
    @Test
    void deleteDepartmentRejectsDepartmentWithChildren() {
        SysDepartment existingDepartment = department(
            2L,
            1L,
            "研发部",
            10,
            1
        );

        /*
         * 模拟要删除的部门存在。
         */
        when(departmentMapper.findAvailableById(2L))
            .thenReturn(existingDepartment);

        /*
         * 模拟当前部门下面有一个子部门。
         */
        when(departmentMapper.countChildren(2L))
            .thenReturn(1L);

        assertThatThrownBy(() ->
            departmentService.deleteDepartment(2L)
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.errorCode())
                    .isEqualTo(
                        ErrorCode.BUSINESS_RULE_VIOLATION
                    );

                assertThat(exception.getMessage())
                    .isEqualTo(
                        "当前部门下存在子部门，不能删除"
                    );
            }
        );

        /*
         * 验证 Service 查询了当前部门。
         */
        verify(departmentMapper)
            .findAvailableById(2L);

        /*
         * 验证 Service 查询了子部门数量。
         */
        verify(departmentMapper)
            .countChildren(2L);

        /*
         * 发现子部门后，应立即停止，
         * 不需要继续查询员工，也不能执行删除。
         */
        verify(departmentMapper, never())
            .countUsers(2L);

        verify(departmentMapper, never())
            .softDelete(2L);
    }


    /**
     * 部门下存在员工时，应拒绝删除。
     */
    @Test
    void deleteDepartmentRejectsDepartmentWithUsers() {
        SysDepartment existingDepartment = department(
            2L,
            1L,
            "研发部",
            10,
            1
        );

        /*
         * 模拟要删除的部门存在。
         */
        when(departmentMapper.findAvailableById(2L))
            .thenReturn(existingDepartment);

        /*
         * 模拟当前部门下没有子部门。
         *
         * 只有子部门数量为 0，
         * Service 才会继续检查员工数量。
         */
        when(departmentMapper.countChildren(2L))
            .thenReturn(0L);

        /*
         * 模拟当前部门下存在 3 名员工。
         */
        when(departmentMapper.countUsers(2L))
            .thenReturn(3L);

        assertThatThrownBy(() ->
            departmentService.deleteDepartment(2L)
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.errorCode())
                    .isEqualTo(
                        ErrorCode.BUSINESS_RULE_VIOLATION
                    );

                assertThat(exception.getMessage())
                    .isEqualTo(
                        "当前部门下存在员工，不能删除"
                    );
            }
        );

        /*
         * 验证删除前的检查顺序。
         */
        verify(departmentMapper)
            .findAvailableById(2L);

        verify(departmentMapper)
            .countChildren(2L);

        verify(departmentMapper)
            .countUsers(2L);

        /*
         * 存在员工时，不能执行逻辑删除。
         */
        verify(departmentMapper, never())
            .softDelete(2L);
    }

    /**
     * 部门下没有子部门和员工时，应成功执行逻辑删除。
     */
    @Test
    void deleteDepartmentSoftDeletesDepartment() {
        SysDepartment existingDepartment = department(
            2L,
            1L,
            "研发部",
            10,
            1
        );

        /*
         * 模拟要删除的部门存在。
         */
        when(departmentMapper.findAvailableById(2L))
            .thenReturn(existingDepartment);

        /*
         * 模拟当前部门下没有子部门。
         */
        when(departmentMapper.countChildren(2L))
            .thenReturn(0L);

        /*
         * 模拟当前部门下没有员工。
         */
        when(departmentMapper.countUsers(2L))
            .thenReturn(0L);

        /*
         * 模拟逻辑删除成功。
         *
         * 返回 1 表示成功更新了一条数据库记录。
         */
        when(departmentMapper.softDelete(2L))
            .thenReturn(1);

        departmentService.deleteDepartment(2L);

        /*
         * 验证删除前的检查都执行了。
         */
        verify(departmentMapper)
            .findAvailableById(2L);

        verify(departmentMapper)
            .countChildren(2L);

        verify(departmentMapper)
            .countUsers(2L);

        /*
         * 验证最终执行了逻辑删除。
         */
        verify(departmentMapper)
            .softDelete(2L);
    }
    /**
     * 创建一个用于测试的部门实体。
     */
    private SysDepartment department(
        Long id,
        Long parentId,
        String name,
        Integer sortOrder,
        Integer status
    ) {
        LocalDateTime now =
            LocalDateTime.of(2026, 7, 21, 10, 0);

        SysDepartment department = new SysDepartment();
        department.setId(id);
        department.setParentId(parentId);
        department.setName(name);
        department.setSortOrder(sortOrder);
        department.setStatus(status);
        department.setCreatedAt(now);
        department.setUpdatedAt(now);
        department.setDeleted(0);

        return department;
    }
}
