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
import com.personaowl.oa.flow.domain.vo.FlowActionResponse;
import com.personaowl.oa.flow.domain.vo.FlowRequestDetailResponse;
import com.personaowl.oa.flow.search.FlowSearchService;
import com.personaowl.oa.flow.mapper.FlowActionLogMapper;
import com.personaowl.oa.flow.mapper.FlowAttendanceMapper;
import com.personaowl.oa.flow.mapper.FlowRequestMapper;
import com.personaowl.oa.flow.mapper.FlowUserDirectoryMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FlowApprovalService {
    private final FlowRequestMapper requestMapper;
    private final FlowActionLogMapper actionLogMapper;
    private final FlowUserDirectoryMapper userDirectoryMapper;
    private final FlowSearchService flowSearchService;
    private final FlowAttendanceMapper attendanceMapper;
    private static final Set<String> LEAVE_TYPES = Set.of("PERSONAL", "SICK", "ANNUAL", "COMPENSATORY");
    private static final Set<String> OVERTIME_COMPENSATIONS = Set.of("PAY", "COMPENSATORY");

    public FlowApprovalService(
            FlowRequestMapper requestMapper,
            FlowActionLogMapper actionLogMapper,
            FlowUserDirectoryMapper userDirectoryMapper) {
        this(requestMapper, actionLogMapper, userDirectoryMapper, null, null);
    }

    @Autowired
    public FlowApprovalService(
            FlowRequestMapper requestMapper,
            FlowActionLogMapper actionLogMapper,
            FlowUserDirectoryMapper userDirectoryMapper,
            FlowSearchService flowSearchService,
            FlowAttendanceMapper attendanceMapper) {
        this.requestMapper = requestMapper;
        this.actionLogMapper = actionLogMapper;
        this.userDirectoryMapper = userDirectoryMapper;
        this.flowSearchService = flowSearchService;
        this.attendanceMapper = attendanceMapper;
    }

    @Transactional
    public FlowRequestResponse submit(FlowRequestType type, FlowSubmitRequest request, Long applicantId) {
        Long userId = requireUserId(applicantId);
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "结束时间必须晚于开始时间");
        }
        if (requestMapper.countOverlapping(userId, request.startTime(), request.endTime()) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "该时间段已有待审批或已通过的申请");
        }
        String leaveType = type == FlowRequestType.LEAVE
                ? normalizeRequiredOption(request.leaveType(), LEAVE_TYPES, "请假类型")
                : null;
        String compensation = type == FlowRequestType.OVERTIME
                ? normalizeRequiredOption(request.overtimeCompensation(), OVERTIME_COMPENSATIONS, "加班补偿方式")
                : null;
        Long approverId = resolveApprover(userId);

        LocalDateTime now = LocalDateTime.now();
        FlowRequest entity = new FlowRequest();
        entity.setApplicantId(userId);
        entity.setRequestType(type.name());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setReason(request.reason().trim());
        entity.setLeaveType(leaveType);
        entity.setOvertimeCompensation(compensation);
        entity.setDurationMinutes(Math.toIntExact(ChronoUnit.MINUTES.between(request.startTime(), request.endTime())));
        entity.setStatus(FlowRequestStatus.PENDING.name());
        entity.setCurrentApproverId(approverId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (requestMapper.insert(entity) != 1) {
            throw new IllegalStateException("创建审批申请失败");
        }
        insertAction(entity.getId(), userId, "SUBMIT", "提交申请", now);
        synchronize(entity, null);
        return toResponse(entity, null);
    }

    @Transactional(readOnly = true)
    public List<FlowApproverResponse> listApprovers(Long currentUserId) {
        Long approverId = resolveApprover(requireUserId(currentUserId));
        FlowApproverResponse approver = userDirectoryMapper.findApprover(approverId);
        return approver == null ? List.of() : List.of(approver);
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
    public FlowRequestDetailResponse detail(Long requestId, Long currentUserId) {
        Long userId = requireUserId(currentUserId);
        FlowRequest request = requireRequest(requestId);
        boolean related = userId.equals(request.getApplicantId())
                || userId.equals(request.getCurrentApproverId())
                || actionLogMapper.countByRequestAndOperator(requestId, userId) > 0;
        if (!related) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        List<FlowActionResponse> timeline = actionLogMapper.findTimeline(requestId).stream()
                .map(action -> new FlowActionResponse(
                        action.getId(), action.getAction(), action.getOperatorId(),
                        findDisplayName(action.getOperatorId()), action.getComment(), action.getOperatedAt()))
                .toList();
        return new FlowRequestDetailResponse(
                toResponse(request, actionLogMapper.findLatest(requestId)), timeline);
    }

    @Transactional
    public FlowRequestResponse withdraw(Long requestId, Long applicantId) {
        Long userId = requireUserId(applicantId);
        FlowRequest request = requireRequest(requestId);
        if (!userId.equals(request.getApplicantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能撤回本人提交的申请");
        }
        if (!FlowRequestStatus.PENDING.name().equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.APPROVAL_STATE_INVALID, "只有待审批申请可以撤回");
        }
        LocalDateTime now = LocalDateTime.now();
        if (requestMapper.withdraw(requestId, userId, now) != 1) {
            throw new BusinessException(ErrorCode.APPROVAL_STATE_INVALID, "申请状态已变化，请刷新后重试");
        }
        insertAction(requestId, userId, "WITHDRAW", "申请人主动撤回", now);
        request.setStatus(FlowRequestStatus.WITHDRAWN.name());
        request.setCurrentApproverId(null);
        request.setUpdatedAt(now);
        FlowActionLog action = actionLogMapper.findLatest(requestId);
        synchronize(request, action);
        return toResponse(request, action);
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
        if (decision == FlowDecision.REJECT
                && (approval.comment() == null || approval.comment().isBlank())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "驳回申请时必须填写审批意见");
        }
        String newStatus = decision == FlowDecision.APPROVE
                ? FlowRequestStatus.APPROVED.name()
                : FlowRequestStatus.REJECTED.name();
        LocalDateTime now = LocalDateTime.now();
        int affected = requestMapper.completeApproval(requestId, userId, newStatus, now);
        if (affected != 1) {
            throw new BusinessException(ErrorCode.APPROVAL_STATE_INVALID, "申请已被处理，请刷新后重试");
        }

        FlowActionLog action = insertAction(
                requestId, userId, decision.name(), normalizeComment(approval.comment()), now);

        request.setStatus(newStatus);
        request.setCurrentApproverId(null);
        request.setUpdatedAt(now);
        if (decision == FlowDecision.APPROVE && FlowRequestType.LEAVE.name().equals(request.getRequestType())) {
            synchronizeApprovedLeave(request, now);
        }
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

    private Long resolveApprover(Long applicantId) {
        Long approverId = userDirectoryMapper.findDepartmentManager(applicantId);
        if (!isValidUserId(approverId)) approverId = userDirectoryMapper.findFallbackAdmin(applicantId);
        if (!isValidUserId(approverId)) approverId = userDirectoryMapper.findFallbackManager(applicantId);
        if (!isValidUserId(approverId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "未找到可用审批人，请先配置部门负责人");
        }
        return approverId;
    }

    private boolean isValidUserId(Long userId) {
        return userId != null && userId > 0;
    }

    private String normalizeRequiredOption(String value, Set<String> allowed, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "不能为空");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "不合法");
        }
        return normalized;
    }

    private FlowActionLog insertAction(
            Long requestId, Long operatorId, String actionName, String comment, LocalDateTime operatedAt) {
        FlowActionLog action = new FlowActionLog();
        action.setRequestId(requestId);
        action.setOperatorId(operatorId);
        action.setAction(actionName);
        action.setComment(comment);
        action.setOperatedAt(operatedAt);
        if (actionLogMapper.insert(action) != 1) {
            throw new IllegalStateException("保存审批记录失败");
        }
        return action;
    }

    private void synchronizeApprovedLeave(FlowRequest request, LocalDateTime approvedAt) {
        if (attendanceMapper == null) return;
        LocalDate date = request.getStartTime().toLocalDate();
        LocalDate endDate = request.getEndTime().toLocalDate();
        while (!date.isAfter(endDate)) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                attendanceMapper.insertApprovedLeave(
                        IdWorker.getId(), request.getApplicantId(), date, approvedAt);
            }
            date = date.plusDays(1);
        }
    }
}
