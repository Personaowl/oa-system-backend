package com.personaowl.oa.ai.dto;

public record AiChatLogQueryDTO(
        Integer page,
        Integer size,
        Long userId,
        String keyword,
        String knowledgeDomain,
        Boolean hitFlag) {
}
