package com.personaowl.oa.flow.api;

import com.personaowl.oa.common.core.api.ApiResponse;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.common.core.web.RequestHeaders;
import com.personaowl.oa.flow.domain.dto.FlowApprovalRequest;
import com.personaowl.oa.flow.domain.dto.FlowSubmitRequest;
import com.personaowl.oa.flow.domain.enums.FlowRequestType;
import com.personaowl.oa.flow.domain.vo.FlowRequestResponse;
import com.personaowl.oa.flow.domain.vo.FlowApproverResponse;
import com.personaowl.oa.flow.service.FlowApprovalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/flows")
public class FlowApprovalController {
    private static final String APPROVE_PERMISSION = "flow:task:approve";
    private final FlowApprovalService flowApprovalService;

    public FlowApprovalController(FlowApprovalService flowApprovalService) {
        this.flowApprovalService = flowApprovalService;
    }

    @PostMapping("/leave-requests")
    public ApiResponse<FlowRequestResponse> submitLeave(
            @Valid @RequestBody FlowSubmitRequest request,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(
                flowApprovalService.submit(FlowRequestType.LEAVE, request, userId), traceId);
    }

    @PostMapping("/overtime-requests")
    public ApiResponse<FlowRequestResponse> submitOvertime(
            @Valid @RequestBody FlowSubmitRequest request,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(
                flowApprovalService.submit(FlowRequestType.OVERTIME, request, userId), traceId);
    }

    @GetMapping("/requests/mine")
    public ApiResponse<List<FlowRequestResponse>> mine(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(flowApprovalService.listMine(userId), traceId);
    }

    @GetMapping("/approvers")
    public ApiResponse<List<FlowApproverResponse>> approvers(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(flowApprovalService.listApprovers(userId), traceId);
    }

    @GetMapping("/requests/{id}")
    public ApiResponse<FlowRequestResponse> detail(
            @PathVariable Long id,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        return ApiResponse.success(flowApprovalService.detail(id, userId), traceId);
    }

    @GetMapping("/tasks/todo")
    public ApiResponse<List<FlowRequestResponse>> todo(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        requireApprovePermission(permissions);
        return ApiResponse.success(flowApprovalService.listTodo(userId), traceId);
    }

    @GetMapping("/tasks/done")
    public ApiResponse<List<FlowRequestResponse>> done(
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        requireApprovePermission(permissions);
        return ApiResponse.success(flowApprovalService.listDone(userId), traceId);
    }

    @PostMapping("/tasks/{id}/approve")
    public ApiResponse<FlowRequestResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody FlowApprovalRequest request,
            @RequestHeader(value = RequestHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = RequestHeaders.PERMISSIONS, required = false) String permissions,
            @RequestHeader(value = RequestHeaders.TRACE_ID, required = false) String traceId) {
        requireApprovePermission(permissions);
        return ApiResponse.success(flowApprovalService.approve(id, request, userId), traceId);
    }

    private void requireApprovePermission(String permissions) {
        if (permissions == null || permissions.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Set<String> permissionSet = new HashSet<>(Arrays.asList(permissions.split("[,;\\s]+")));
        if (!permissionSet.contains(APPROVE_PERMISSION)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
