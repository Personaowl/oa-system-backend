package com.personaowl.oa.ai.dto;

public record AiIndexTaskQueryDTO(
        Integer page,
        Integer size,
        String status,
        String taskType,
        Long docId) {
}
