package com.personaowl.oa.asset.infrastructure;

import com.personaowl.oa.asset.domain.AssetUserScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AssetAccessMapper {
    @Select("SELECT id AS userId, department_id AS departmentId, display_name AS displayName FROM sys_user WHERE id=#{userId} AND deleted=0 AND status=1 LIMIT 1")
    AssetUserScope findUser(@Param("userId") Long userId);

    @Select("""
        SELECT DISTINCT department_id FROM sys_department_manager WHERE user_id=#{userId}
        UNION SELECT id FROM sys_department WHERE manager_id=#{userId} AND deleted=0
        """)
    List<Long> findManagedDepartmentIds(@Param("userId") Long userId);
}
