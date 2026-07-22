package com.personaowl.oa.flow.domain.vo;

public record FlowApproverResponse(
        Long id,
        String username,
        String displayName,
        Long departmentId,
        String departmentName
) {
}
