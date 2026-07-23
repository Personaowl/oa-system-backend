package com.personaowl.oa.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @NotBlank(message = "当前密码不能为空")
        String currentPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 72, message = "新密码长度必须为 6-72 位")
        String newPassword
) {
}
