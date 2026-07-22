package com.personaowl.oa.ai.application.service.impl;

import com.personaowl.oa.ai.application.service.AiIndexTaskService;
import com.personaowl.oa.ai.domain.entity.AiIndexTask;
import com.personaowl.oa.ai.domain.vo.AiIndexTaskVO;
import com.personaowl.oa.ai.domain.vo.PageResultVO;
import com.personaowl.oa.ai.infrastructure.mapper.AiIndexTaskMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiIndexTaskServiceImpl implements AiIndexTaskService {

    private final AiIndexTaskMapper aiIndexTaskMapper;

    public AiIndexTaskServiceImpl(AiIndexTaskMapper aiIndexTaskMapper) {
        this.aiIndexTaskMapper = aiIndexTaskMapper;
    }

    @Override
    public PageResultVO<AiIndexTaskVO> page(Integer page, Integer size, String status, String taskType, Long docId) {
        List<AiIndexTask> tasks = aiIndexTaskMapper.selectPage(status, taskType, docId, offset(page, size), size == null ? 20 : size);
        List<AiIndexTaskVO> vos = tasks.stream().map(this::toVO).toList();
        return new PageResultVO<>(vos, page, size, (long) vos.size());
    }

    @Override
    public AiIndexTaskVO get(Long id) {
        AiIndexTask entity = aiIndexTaskMapper.selectById(id);
        return entity == null ? AiIndexTaskVO.empty(id) : toVO(entity);
    }

    private AiIndexTaskVO toVO(AiIndexTask entity) {
        return new AiIndexTaskVO(entity.getId(), entity.getTaskNo(), entity.getDocId(), entity.getTaskType(), entity.getStatus(),
                entity.getErrorMessage(), entity.getRetryCount(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(java.time.ZoneOffset.ofHours(8)),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().atOffset(java.time.ZoneOffset.ofHours(8)));
    }

    private int offset(Integer page, Integer size) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 20 : size;
        return (p - 1) * s;
    }
}
