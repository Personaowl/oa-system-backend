package com.personaowl.oa.ai.api;

import com.personaowl.oa.ai.application.service.AiChatService;
import com.personaowl.oa.ai.domain.vo.AiChatLogVO;
import com.personaowl.oa.ai.domain.vo.PageResultVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/chat-logs")
@Validated
public class AiChatLogController {

    private final AiChatService aiChatService;

    public AiChatLogController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping
    public PageResultVO<AiChatLogVO> page(@RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer size,
                                          @RequestParam(required = false) Long userId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String knowledgeDomain,
                                          @RequestParam(required = false) Boolean hitFlag) {
        Long operatorId = null;
        return aiChatService.pageLogs(operatorId, page, size, userId, keyword, knowledgeDomain, hitFlag);
    }

    @GetMapping("/{id}")
    public AiChatLogVO detail(@PathVariable Long id) {
        Long operatorId = null;
        return aiChatService.getLog(operatorId, id);
    }
}
