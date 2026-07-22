package com.personaowl.oa.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatSessionUpdateDTO(
        @NotBlank(message = "状态不能为空") String status) {
}
