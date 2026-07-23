package com.personaowl.oa.document.api.dto;

public record DocumentWorkspaceResponse(
    Long departmentId,
    String departmentName,
    Long memberCount,
    Long documentCount,
    boolean manageable
) {
}
