package com.personaowl.oa.flow.domain.vo;

import com.personaowl.oa.flow.domain.entity.FlowActionLog;
import com.personaowl.oa.flow.domain.entity.FlowRequest;

import java.time.LocalDateTime;

public record FlowRequestResponse(
        Long id,
        Long applicantId,
        String applicantName,
        String requestType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason,
        String leaveType,
        String overtimeCompensation,
        Integer durationMinutes,
        String status,
        Long currentApproverId,
        String currentApproverName,
        String decision,
        String approvalComment,
        Long decidedBy,
        String decidedByName,
        LocalDateTime decidedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FlowRequestResponse from(
            FlowRequest request,
            FlowActionLog action,
            String applicantName,
            String currentApproverName,
            String decidedByName) {
        return new FlowRequestResponse(
                request.getId(), request.getApplicantId(), applicantName, request.getRequestType(),
                request.getStartTime(), request.getEndTime(), request.getReason(),
                request.getLeaveType(), request.getOvertimeCompensation(), request.getDurationMinutes(),
                request.getStatus(), request.getCurrentApproverId(), currentApproverName,
                action == null ? null : action.getAction(),
                action == null ? null : action.getComment(),
                action == null ? null : action.getOperatorId(),
                decidedByName,
                action == null ? null : action.getOperatedAt(),
                request.getCreatedAt(), request.getUpdatedAt());
    }
}
