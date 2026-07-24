package com.personaowl.oa.asset.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupplyUpsertRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 64) String category,
    @NotBlank @Size(max = 32) String unit,
    @NotNull @Min(0) Integer stockQuantity,
    @NotNull @Min(0) Integer safetyStock,
    @NotNull Integer status,
    Integer version
) {}
