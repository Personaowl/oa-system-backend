package com.personaowl.oa.ai.api;

import com.personaowl.oa.ai.application.service.AiChatService;
import com.personaowl.oa.ai.dto.AiChatRequestDTO;
import com.personaowl.oa.ai.dto.AiChatSessionQueryDTO;
import com.personaowl.oa.ai.domain.vo.AiChatResponseVO;
import com.personaowl.oa.ai.domain.vo.AiChatSessionVO;
import com.personaowl.oa.ai.domain.vo.PageResultVO;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ai")
@Validated
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chats")
    public AiChatResponseVO chat(@RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                 @Valid @RequestBody AiChatRequestDTO request) {
        Long userId = null;
        return aiChatService.chat(userId, traceId, request.question(), toLong(request.sessionId()), request.knowledgeDomain(), request.topK(), request.stream());
    }

    @PostMapping(value = "/chats/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AiChatResponseVO> chatStream(@RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                             @Valid @RequestBody AiChatRequestDTO request) {
        Long userId = null;
        AiChatResponseVO response = aiChatService.chat(userId, traceId, request.question(), toLong(request.sessionId()), request.knowledgeDomain(), request.topK(), true);
        return Flux.just(response);
    }

    @GetMapping("/chat-sessions")
    public PageResultVO<AiChatSessionVO> listSessions(AiChatSessionQueryDTO query) {
        Long userId = null;
        return aiChatService.pageSessions(userId, query.page(), query.size(), query.keyword(), query.status());
    }

    @GetMapping("/chat-sessions/{id}")
    public AiChatSessionVO getSession(@PathVariable Long id) {
        Long userId = null;
        return aiChatService.getSession(userId, id);
    }

    @PatchMapping("/chat-sessions/{id}")
    public AiChatSessionVO archiveSession(@PathVariable Long id, @RequestParam String status) {
        Long userId = null;
        return aiChatService.archiveSession(userId, id);
    }

    @DeleteMapping("/chat-sessions/{id}")
    public void deleteSession(@PathVariable Long id) {
        Long userId = null;
        aiChatService.deleteSession(userId, id);
    }

    private Long toLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
