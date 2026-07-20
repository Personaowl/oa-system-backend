package com.personaowl.oa.ai.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.web.RequestHeaders;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    private final ChatClient chatClient;

    public AiChatController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是 OA 办公系统助手。回答应简洁、准确；不知道时明确说明，不编造制度。")
                .build();
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        String answer = chatClient.prompt().user(request.message()).call().content();
        return ApiResponse.success(new ChatResponse(answer), traceId);
    }

    public record ChatRequest(@NotBlank(message = "消息不能为空") String message) {
    }

    public record ChatResponse(String answer) {
    }
}
