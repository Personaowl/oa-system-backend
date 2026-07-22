package com.personaowl.oa.ai.domain.vo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AiKnowledgeDocVO(
        Long id,
        String docTitle,
        String docDomain,
        String docVersion,
        String fileName,
        String fileUrl,
        String contentHash,
        String status,
        String sourceType,
        LocalDate effectiveDate,
        Long approvedBy,
        OffsetDateTime approvedAt,
        OffsetDateTime createdAt,
        List<AiKnowledgeChunkVO> chunks) {

    public static AiKnowledgeDocVO empty() {
        return new AiKnowledgeDocVO(null, null, null, null, null, null, null, null, null, null, null, null, null, List.of());
    }
}
