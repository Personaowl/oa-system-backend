package com.personaowl.oa.ai.infrastructure.mapper;

import com.personaowl.oa.ai.domain.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiChatSessionMapper {

    int insert(AiChatSession entity);

    int updateById(AiChatSession entity);

    AiChatSession selectById(Long id);

    List<AiChatSession> selectPage(Long userId, String keyword, String status, Integer offset, Integer size);
}
