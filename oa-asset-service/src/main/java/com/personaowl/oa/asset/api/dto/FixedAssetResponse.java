package com.personaowl.oa.asset.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FixedAssetResponse(
    Long id, String assetCode, String name, String category, String specification,
    LocalDate purchaseDate, BigDecimal originalValue, String status,
    Long custodianId, String custodianName, Long departmentId, String departmentName,
    String location, String remark, Integer version, LocalDateTime updatedAt
) {}
