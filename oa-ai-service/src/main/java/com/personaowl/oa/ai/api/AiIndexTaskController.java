package com.personaowl.oa.ai.api;

import com.personaowl.oa.ai.application.service.AiIndexTaskService;
import com.personaowl.oa.ai.domain.vo.AiIndexTaskVO;
import com.personaowl.oa.ai.domain.vo.PageResultVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/index-tasks")
@Validated
public class AiIndexTaskController {

    private final AiIndexTaskService aiIndexTaskService;

    public AiIndexTaskController(AiIndexTaskService aiIndexTaskService) {
        this.aiIndexTaskService = aiIndexTaskService;
    }

    @GetMapping
    public PageResultVO<AiIndexTaskVO> page(@RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String taskType,
                                            @RequestParam(required = false) Long docId) {
        return aiIndexTaskService.page(page, size, status, taskType, docId);
    }

    @GetMapping("/{id}")
    public AiIndexTaskVO detail(@PathVariable Long id) {
        return aiIndexTaskService.get(id);
    }
}
