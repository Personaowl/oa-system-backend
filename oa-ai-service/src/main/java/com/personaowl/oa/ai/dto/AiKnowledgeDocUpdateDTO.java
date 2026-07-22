package com.personaowl.oa.ai.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AiKnowledgeDocUpdateDTO(
        @NotBlank(message = "文档标题不能为空") String docTitle,
        @NotBlank(message = "知识域不能为空") String docDomain,
        @NotBlank(message = "文档版本不能为空") String docVersion,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate) {
}
