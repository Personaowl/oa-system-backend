package com.personaowl.oa.asset.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FixedAssetAssignRequest(
    @NotNull Long custodianId,
    @Size(max = 128) String location,
    @NotNull Integer version
) {}
