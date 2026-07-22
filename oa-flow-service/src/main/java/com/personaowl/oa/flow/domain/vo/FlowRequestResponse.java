package com.personaowl.oa.flow.domain.vo;

import com.personaowl.oa.flow.domain.entity.FlowActionLog;
import com.personaowl.oa.flow.domain.entity.FlowRequest;

import java.time.LocalDateTime;

public record FlowRequestResponse(
        Long id,
        Long applicantId,
        String requestType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason,
        String status,
        Long currentApproverId,
        String decision,
        String approvalComment,
        Long decidedBy,
        LocalDateTime decidedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FlowRequestResponse from(FlowRequest request, FlowActionLog action) {
        return new FlowRequestResponse(
                request.getId(), request.getApplicantId(), request.getRequestType(),
                request.getStartTime(), request.getEndTime(), request.getReason(),
                request.getStatus(), request.getCurrentApproverId(),
                action == null ? null : action.getAction(),
                action == null ? null : action.getComment(),
                action == null ? null : action.getOperatorId(),
                action == null ? null : action.getOperatedAt(),
                request.getCreatedAt(), request.getUpdatedAt());
    }
}
