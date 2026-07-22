package com.personaowl.oa.ai.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record AiChatLogVO(
        Long id,
        Long sessionId,
        Long userId,
        String question,
        String answer,
        List<AiChatResponseVO.CitationVO> citations,
        String modelName,
        Integer topK,
        Double confidenceScore,
        Boolean hitFlag,
        Integer latencyMs,
        OffsetDateTime createdAt) {

    public static AiChatLogVO empty(Long id) {
        return new AiChatLogVO(id, null, null, null, null, List.of(), null, null, null, null, null, null);
    }
}
