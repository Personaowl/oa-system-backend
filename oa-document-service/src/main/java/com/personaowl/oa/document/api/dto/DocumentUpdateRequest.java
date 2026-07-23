package com.personaowl.oa.document.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentUpdateRequest(
    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题不能超过 200 个字符")
    String title,

    @NotNull(message = "文档内容不能为空")
    JsonNode content,

    @NotNull(message = "文档版本不能为空")
    Integer version
) {
}
