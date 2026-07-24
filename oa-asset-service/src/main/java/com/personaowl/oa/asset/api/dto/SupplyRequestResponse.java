package com.personaowl.oa.asset.api.dto;

import java.time.LocalDateTime;

public record SupplyRequestResponse(
    Long id, String requestNo, Long applicantId, String applicantName,
    Long departmentId, String departmentName, Long supplyId, String supplyName,
    String supplyUnit, Integer quantity, String reason, String status,
    String reviewerName, String reviewComment, LocalDateTime reviewedAt,
    LocalDateTime issuedAt, LocalDateTime createdAt, Integer version
) {}
