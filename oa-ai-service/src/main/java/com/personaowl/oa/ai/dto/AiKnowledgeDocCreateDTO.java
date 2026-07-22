package com.personaowl.oa.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AiKnowledgeDocCreateDTO(
        @NotBlank(message = "文档标题不能为空") String docTitle,
        @NotBlank(message = "知识域不能为空") String docDomain,
        @NotBlank(message = "文档版本不能为空") String docVersion,
        LocalDate effectiveDate,
        @NotBlank(message = "来源类型不能为空") String sourceType) {
}
