package com.personaowl.oa.flow.domain.vo;

import java.time.LocalDateTime;

public record FlowActionResponse(
        Long id,
        String action,
        Long operatorId,
        String operatorName,
        String comment,
        LocalDateTime operatedAt
) {
}
