package com.personaowl.oa.flow.domain.vo;

import java.time.LocalDateTime;

public record FlowSearchItemResponse(
        Long id,
        String title,
        String contentSnippet,
        String requestType,
        String status,
        Long applicantId,
        Long currentApproverId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String highlightedTitle,
        String highlightedContent
) {
}
