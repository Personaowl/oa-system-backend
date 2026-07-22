package com.personaowl.oa.ai.infrastructure.mapper;

import com.personaowl.oa.ai.domain.entity.AiChatLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiChatLogMapper {

    int insert(AiChatLog entity);

    AiChatLog selectById(Long id);

    List<AiChatLog> selectPage(Long userId, Long queryUserId, String keyword, String knowledgeDomain, Integer hitFlag, Integer offset, Integer size);
}
