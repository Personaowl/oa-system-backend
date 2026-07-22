package com.personaowl.oa.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserUpdateRequest(
        @NotBlank @Size(max = 64) String username,
        @Size(min = 6, max = 72) String newPassword,
        @NotBlank @Size(max = 64) String displayName,
        @Min(1) Long departmentId,
        @Size(max = 32) String phone,
        @Email @Size(max = 128) String email,
        @Min(0) @Max(1) Integer status,
        @NotEmpty Set<@Min(1) Long> roleIds
) {
}
