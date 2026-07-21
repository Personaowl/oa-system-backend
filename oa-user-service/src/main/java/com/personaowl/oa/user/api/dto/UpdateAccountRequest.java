package com.personaowl.oa.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @Pattern(regexp = "^$|^[A-Za-z0-9_]{3,32}$", message = "Username must contain 3-32 letters, numbers, or underscores")
        String username,
        @Pattern(regexp = "^$|^.{6,72}$", message = "New password must contain 6-72 characters")
        String newPassword
) {
}
