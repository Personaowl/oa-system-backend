package com.personaowl.oa.ai.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record AiChatSessionVO(
        Long id,
        String sessionNo,
        String sessionTitle,
        String knowledgeDomain,
        String latestQuestion,
        String latestAnswer,
        Integer messageCount,
        String status,
        OffsetDateTime createdAt,
        List<AiChatMessageVO> messages) {

    public static AiChatSessionVO empty(Long id) {
        return new AiChatSessionVO(id, null, null, null, null, null, 0, null, null, List.of());
    }

    public record AiChatMessageVO(String role, String content, List<AiChatResponseVO.CitationVO> citations, OffsetDateTime createdAt) {
    }
}
