package com.personaowl.oa.ai.dto;

public record AiKnowledgeDocQueryDTO(
        Integer page,
        Integer size,
        String keyword,
        String docDomain,
        String status) {
}
