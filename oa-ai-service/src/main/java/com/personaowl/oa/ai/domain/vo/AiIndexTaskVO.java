package com.personaowl.oa.ai.domain.vo;

import java.time.OffsetDateTime;

public record AiIndexTaskVO(
        Long id,
        String taskNo,
        Long docId,
        String taskType,
        String status,
        String errorMessage,
        Integer retryCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static AiIndexTaskVO empty(Long id) {
        return new AiIndexTaskVO(id, null, null, null, null, null, null, null, null);
    }
}
