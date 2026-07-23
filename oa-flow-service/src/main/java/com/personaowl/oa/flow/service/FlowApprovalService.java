package com.personaowl.oa.flow.service;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.flow.domain.dto.FlowApprovalRequest;
import com.personaowl.oa.flow.domain.dto.FlowSubmitRequest;
import com.personaowl.oa.flow.domain.dto.FlowSearchRequest;
import com.personaowl.oa.flow.domain.entity.FlowActionLog;
import com.personaowl.oa.flow.domain.entity.FlowRequest;
import com.personaowl.oa.flow.domain.enums.FlowDecision;
import com.personaowl.oa.flow.domain.enums.FlowRequestStatus;
import com.personaowl.oa.flow.domain.enums.FlowRequestType;
import com.personaowl.oa.flow.domain.vo.FlowRequestResponse;
import com.personaowl.oa.flow.domain.vo.FlowApproverResponse;
import com.personaowl.oa.flow.domain.vo.FlowSearchPageResponse;
import com.personaowl.oa.flow.search.FlowSearchService;
import com.personaowl.oa.flow.mapper.FlowActionLogMapper;
import com.personaowl.oa.flow.mapper.FlowRequestMapper;
import com.personaowl.oa.flow.mapper.FlowUserDirectoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class FlowApprovalService {
    private final FlowRequestMapper requestMapper;
    private final FlowActionLogMapper actionLogMapper;
    private final FlowUserDirectoryMapper userDirectoryMapper;
    private final FlowSearchService flowSearchService;

    public FlowApprovalService(
            FlowRequestMapper requestMapper,
            FlowActionLogMapper actionLogMapper,
            FlowUserDirectoryMapper userDirectoryMapper) {
        this(requestMapper, actionLogMapper, userDirectoryMapper, null);
    }

    @Autowired
    public FlowApprovalService(
            FlowRequestMapper requestMapper,
            FlowActionLogMapper actionLogMapper,
            FlowUserDirectoryMapper userDirectoryMapper,
            FlowSearchService flowSearchService) {
        this.requestMapper = requestMapper;
        this.actionLogMapper = actionLogMapper;
        this.userDirectoryMapper = userDirectoryMapper;
        this.flowSearchService = flowSearchService;
    }

    @Transactional
    public FlowRequestResponse submit(FlowRequestType type, FlowSubmitRequest request, Long applicantId) {
        Long userId = requireUserId(applicantId);
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "结束时间必须晚于开始时间");
        }
        if (userId.equals(request.approverId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "申请人不能作为自己的审批人");
        }

        LocalDateTime now = LocalDateTime.now();
        FlowRequest entity = new FlowRequest();
        entity.setApplicantId(userId);
        entity.setRequestType(type.name());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setReason(request.reason().trim());
        entity.setStatus(FlowRequestStatus.PENDING.name());
        entity.setCurrentApproverId(request.approverId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (requestMapper.insert(entity) != 1) {
            throw new IllegalStateException("创建审批申请失败");
        }
        synchronize(entity, null);
        return toResponse(entity, null);
    }

    @Transactional(readOnly = true)
    public List<FlowApproverResponse> listApprovers(Long currentUserId) {
        return userDirectoryMapper.findAvailableApprovers(requireUserId(currentUserId));
    }

    @Transactional(readOnly = true)
    public List<FlowRequestResponse> listMine(Long applicantId) {
        return toResponses(requestMapper.findMine(requireUserId(applicantId)));
    }

    @Transactional(readOnly = true)
    public List<FlowRequestResponse> listTodo(Long approverId) {
        return toResponses(requestMapper.findTodo(requireUserId(approverId)));
    }

    @Transactional(readOnly = true)
    public List<FlowRequestResponse> listDone(Long approverId) {
        return toResponses(requestMapper.findDone(requireUserId(approverId)));
    }

    @Transactional(readOnly = true)
    public FlowRequestResponse detail(Long requestId, Long currentUserId) {
        Long userId = requireUserId(currentUserId);
        FlowRequest request = requireRequest(requestId);
        boolean related = userId.equals(request.getApplicantId())
                || userId.equals(request.getCurrentApproverId())
                || actionLogMapper.countByRequestAndOperator(requestId, userId) > 0;
        if (!related) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return toResponse(request, actionLogMapper.findLatest(requestId));
    }

    @Transactional
    public FlowRequestResponse approve(Long requestId, FlowApprovalRequest approval, Long approverId) {
        Long userId = requireUserId(approverId);
        FlowRequest request = requireRequest(requestId);
        if (userId.equals(request.getApplicantId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "审批人不能审批自己的申请");
        }
        if (!FlowRequestStatus.PENDING.name().equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.APPROVAL_STATE_INVALID);
        }
        if (!userId.equals(request.getCurrentApproverId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该申请不属于你的待办任务");
        }

        FlowDecision decision = parseDecision(approval.decision());
        String newStatus = decision == FlowDecision.APPROVE
                ? FlowRequestStatus.APPROVED.name()
                : FlowRequestStatus.REJECTED.name();
        LocalDateTime now = LocalDateTime.now();
        int affected = requestMapper.completeApproval(requestId, userId, newStatus, now);
        if (affected != 1) {
            throw new BusinessException(ErrorCode.APPROVAL_STATE_INVALID, "申请已被处理，请刷新后重试");
        }

        FlowActionLog action = new FlowActionLog();
        action.setRequestId(requestId);
        action.setOperatorId(userId);
        action.setAction(decision.name());
        action.setComment(normalizeComment(approval.comment()));
        action.setOperatedAt(now);
        if (actionLogMapper.insert(action) != 1) {
            throw new IllegalStateException("保存审批记录失败");
        }

        request.setStatus(newStatus);
        request.setCurrentApproverId(null);
        request.setUpdatedAt(now);
        synchronize(request, action);
        return toResponse(request, action);
    }

    @Transactional(readOnly = true)
    public FlowSearchPageResponse search(FlowSearchRequest request, Long currentUserId, boolean allVisible) {
        Long userId = requireUserId(currentUserId);
        if (flowSearchService == null) return new FlowSearchPageResponse(0, List.of());
        return flowSearchService.search(request == null ? new FlowSearchRequest() : request, userId, allVisible);
    }

    @Transactional(readOnly = true)
    public int rebuildSearchIndex() {
        if (flowSearchService == null) return 0;
        List<FlowSearchService.IndexedFlow> flows = requestMapper.findAllForIndex().stream()
                .map(request -> new FlowSearchService.IndexedFlow(request, actionLogMapper.findLatest(request.getId())))
                .toList();
        return flowSearchService.rebuild(flows);
    }

    private void synchronize(FlowRequest request, FlowActionLog action) {
        if (flowSearchService != null) flowSearchService.synchronizeAfterCommit(request, action);
    }

    private List<FlowRequestResponse> toResponses(List<FlowRequest> requests) {
        return requests.stream()
                .map(request -> toResponse(request, actionLogMapper.findLatest(request.getId())))
                .toList();
    }

    private FlowRequestResponse toResponse(FlowRequest request, FlowActionLog action) {
        return FlowRequestResponse.from(
                request,
                action,
                findDisplayName(request.getApplicantId()),
                findDisplayName(request.getCurrentApproverId()),
                action == null ? null : findDisplayName(action.getOperatorId()));
    }

    private String findDisplayName(Long userId) {
        return userId == null ? null : userDirectoryMapper.findDisplayName(userId);
    }

    private FlowRequest requireRequest(Long requestId) {
        if (requestId == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "申请ID不能为空");
        }
        FlowRequest request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "审批申请不存在");
        }
        return request;
    }

    private Long requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private FlowDecision parseDecision(String value) {
        try {
            return FlowDecision.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "审批决定只能是 APPROVE 或 REJECT");
        }
    }

    private String normalizeComment(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }
}
