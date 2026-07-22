package com.personaowl.oa.ai.infrastructure.mapper;

import com.personaowl.oa.ai.domain.entity.AiIndexTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiIndexTaskMapper {

    int insert(AiIndexTask entity);

    int updateById(AiIndexTask entity);

    AiIndexTask selectById(Long id);

    List<AiIndexTask> selectPage(String status, String taskType, Long docId, Integer offset, Integer size);
}
