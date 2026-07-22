package com.personaowl.oa.ai.infrastructure.mapper;

import com.personaowl.oa.ai.domain.entity.AiKnowledgeDoc;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiKnowledgeDocMapper {

    int insert(AiKnowledgeDoc entity);

    int updateById(AiKnowledgeDoc entity);

    AiKnowledgeDoc selectById(Long id);

    List<AiKnowledgeDoc> selectPage(String keyword, String docDomain, String status, Integer offset, Integer size);
}
