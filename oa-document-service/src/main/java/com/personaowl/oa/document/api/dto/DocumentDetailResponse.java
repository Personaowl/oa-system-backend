package com.personaowl.oa.document.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record DocumentDetailResponse(
    Long id,
    Long departmentId,
    String departmentName,
    String title,
    JsonNode content,
    String createdByName,
    String updatedByName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer version,
    boolean manageable
) {
}
