package com.personaowl.oa.asset.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplyReviewRequest(
    @NotBlank String decision,
    @Size(max = 500) String comment
) {}
