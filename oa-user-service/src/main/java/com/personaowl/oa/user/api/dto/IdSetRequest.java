package com.personaowl.oa.user.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record IdSetRequest(@NotNull Set<Long> ids) {
}
