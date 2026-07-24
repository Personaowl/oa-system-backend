package com.personaowl.oa.asset.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupplyApplyRequest(
    @NotNull Long supplyId,
    @NotNull @Min(1) @Max(999) Integer quantity,
    @NotBlank @Size(max = 500) String reason
) {}
