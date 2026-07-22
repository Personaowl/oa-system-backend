package com.personaowl.oa.ai.dto;

public record AiChatSessionQueryDTO(
        Integer page,
        Integer size,
        String keyword,
        String status) {
}
