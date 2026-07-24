package com.personaowl.oa.asset.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FixedAssetUpsertRequest(
    @NotBlank @Size(max = 64) String assetCode,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 64) String category,
    @Size(max = 255) String specification,
    LocalDate purchaseDate,
    @NotNull @DecimalMin("0.00") BigDecimal originalValue,
    @NotBlank String status,
    @Size(max = 128) String location,
    @Size(max = 500) String remark,
    Integer version
) {}
