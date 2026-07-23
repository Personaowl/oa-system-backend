package com.personaowl.oa.document.api.dto;

import java.time.LocalDateTime;

public record DocumentSummaryResponse(
    Long id,
    Long departmentId,
    String departmentName,
    String title,
    String createdByName,
    String updatedByName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer version,
    boolean manageable
) {
}
