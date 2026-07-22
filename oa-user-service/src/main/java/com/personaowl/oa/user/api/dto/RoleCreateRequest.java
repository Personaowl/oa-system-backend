package com.personaowl.oa.user.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleCreateRequest(
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]*", message = "角色编码只能包含字母、数字、下划线和短横线")
        String code,
        @NotBlank @Size(max = 64) String name,
        @Min(0) @Max(1) Integer status) {
}
