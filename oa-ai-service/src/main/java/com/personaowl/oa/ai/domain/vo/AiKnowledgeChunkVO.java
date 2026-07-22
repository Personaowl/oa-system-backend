package com.personaowl.oa.ai.domain.vo;

import java.time.OffsetDateTime;

public record AiKnowledgeChunkVO(
        Long id,
        Long docId,
        Integer chunkNo,
        String chunkTitle,
        String chunkText,
        String status,
        OffsetDateTime createdAt) {
}
