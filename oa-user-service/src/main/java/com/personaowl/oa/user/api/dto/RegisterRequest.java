package com.personaowl.oa.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "Username must contain 3-32 letters, numbers, or underscores")
        String username,
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 72, message = "Password must contain 6-72 characters")
        String password
) {
}
