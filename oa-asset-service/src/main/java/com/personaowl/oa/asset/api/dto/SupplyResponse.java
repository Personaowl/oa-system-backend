package com.personaowl.oa.asset.api.dto;

import java.time.LocalDateTime;

public record SupplyResponse(
    Long id, String name, String category, String unit,
    Integer stockQuantity, Integer safetyStock, Integer status,
    boolean lowStock, Integer version, LocalDateTime updatedAt
) {}
