package com.personaowl.oa.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AiChatRequestDTO(
        @NotBlank(message = "问题不能为空") String question,
        String sessionId,
        String knowledgeDomain,
        @Positive(message = "topK必须大于0") Integer topK,
        @NotNull(message = "stream不能为空") Boolean stream) {
}
