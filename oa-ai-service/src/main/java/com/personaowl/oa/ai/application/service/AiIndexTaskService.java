package com.personaowl.oa.ai.application.service;

import com.personaowl.oa.ai.domain.vo.AiIndexTaskVO;
import com.personaowl.oa.ai.domain.vo.PageResultVO;

public interface AiIndexTaskService {

    PageResultVO<AiIndexTaskVO> page(Integer page, Integer size, String status, String taskType, Long docId);

    AiIndexTaskVO get(Long id);
}
