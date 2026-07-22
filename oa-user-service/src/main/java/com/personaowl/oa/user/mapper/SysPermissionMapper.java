package com.personaowl.oa.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaowl.oa.user.domain.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
    @Select("SELECT id, parent_id, code, name, type, path, created_at, updated_at, deleted FROM sys_permission WHERE deleted = 0 ORDER BY id")
    List<SysPermission> findAllAvailable();

    @Select("<script>SELECT COUNT(*) FROM sys_permission WHERE deleted = 0 AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    long countAvailableIds(@Param("ids") Set<Long> ids);
}
