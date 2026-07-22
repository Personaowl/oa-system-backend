package com.personaowl.oa.ai.infrastructure.mapper;

import com.personaowl.oa.ai.domain.entity.AiKnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiKnowledgeChunkMapper {

    int insertBatch(List<AiKnowledgeChunk> chunks);

    List<AiKnowledgeChunk> selectByDocId(Long docId);

    int deleteByDocId(Long docId);
}
