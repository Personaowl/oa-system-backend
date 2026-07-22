package com.personaowl.oa.flow.service;

import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.flow.domain.dto.FlowApprovalRequest;
import com.personaowl.oa.flow.domain.dto.FlowSubmitRequest;
import com.personaowl.oa.flow.domain.entity.FlowActionLog;
import com.personaowl.oa.flow.domain.entity.FlowRequest;
import com.personaowl.oa.flow.domain.enums.FlowRequestStatus;
import com.personaowl.oa.flow.domain.enums.FlowRequestType;
import com.personaowl.oa.flow.mapper.FlowActionLogMapper;
import com.personaowl.oa.flow.mapper.FlowRequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlowApprovalServiceTest {
    @Mock
    private FlowRequestMapper requestMapper;
    @Mock
    private FlowActionLogMapper actionLogMapper;

    private FlowApprovalService service;
    private LocalDateTime startTime;

    @BeforeEach
    void setUp() {
        service = new FlowApprovalService(requestMapper, actionLogMapper);
        startTime = LocalDateTime.of(2026, 7, 23, 9, 0);
    }

    @Test
    void submitLeaveCreatesPendingRequest() {
        when(requestMapper.insert(any(FlowRequest.class))).thenAnswer(invocation -> {
            FlowRequest request = invocation.getArgument(0);
            request.setId(10L);
            return 1;
        });

        var response = service.submit(
                FlowRequestType.LEAVE,
                new FlowSubmitRequest(startTime, startTime.plusHours(8), " 年假 ", 2L),
                1L);

        assertEquals(10L, response.id());
        assertEquals("LEAVE", response.requestType());
        assertEquals("PENDING", response.status());
        assertEquals("年假", response.reason());
        assertEquals(2L, response.currentApproverId());
    }

    @Test
    void submitRejectsInvalidTimeRange() {
        FlowSubmitRequest request = new FlowSubmitRequest(
                startTime, startTime.minusMinutes(1), "请假", 2L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submit(FlowRequestType.LEAVE, request, 1L));

        assertEquals(ErrorCode.INVALID_ARGUMENT, exception.errorCode());
    }

    @Test
    void submitRejectsSelfApproval() {
        FlowSubmitRequest request = new FlowSubmitRequest(
                startTime, startTime.plusHours(1), "加班", 1L);

        assertThrows(BusinessException.class,
                () -> service.submit(FlowRequestType.OVERTIME, request, 1L));
    }

    @Test
    void approveCompletesPendingRequestAndWritesLog() {
        FlowRequest pending = pendingRequest();
        when(requestMapper.selectById(10L)).thenReturn(pending);
        when(requestMapper.completeApproval(eq(10L), eq(2L), eq("APPROVED"), any()))
                .thenReturn(1);
        when(actionLogMapper.insert(any(FlowActionLog.class))).thenReturn(1);

        var response = service.approve(
                10L, new FlowApprovalRequest("approve", "同意"), 2L);

        assertEquals("APPROVED", response.status());
        assertEquals("APPROVE", response.decision());
        assertEquals("同意", response.approvalComment());
        assertEquals(2L, response.decidedBy());
        assertNull(response.currentApproverId());
    }

    @Test
    void rejectCompletesPendingRequest() {
        FlowRequest pending = pendingRequest();
        when(requestMapper.selectById(10L)).thenReturn(pending);
        when(requestMapper.completeApproval(eq(10L), eq(2L), eq("REJECTED"), any()))
                .thenReturn(1);
        when(actionLogMapper.insert(any(FlowActionLog.class))).thenReturn(1);

        var response = service.approve(
                10L, new FlowApprovalRequest("REJECT", "材料不足"), 2L);

        assertEquals("REJECTED", response.status());
        assertEquals("REJECT", response.decision());
    }

    @Test
    void approvalRejectsWrongApprover() {
        when(requestMapper.selectById(10L)).thenReturn(pendingRequest());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.approve(
                        10L, new FlowApprovalRequest("APPROVE", null), 3L));

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode());
    }

    @Test
    void approvalRejectsAlreadyCompletedRequest() {
        FlowRequest completed = pendingRequest();
        completed.setStatus(FlowRequestStatus.APPROVED.name());
        completed.setCurrentApproverId(null);
        when(requestMapper.selectById(10L)).thenReturn(completed);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.approve(
                        10L, new FlowApprovalRequest("APPROVE", null), 2L));

        assertEquals(ErrorCode.APPROVAL_STATE_INVALID, exception.errorCode());
    }

    private FlowRequest pendingRequest() {
        FlowRequest request = new FlowRequest();
        request.setId(10L);
        request.setApplicantId(1L);
        request.setRequestType(FlowRequestType.LEAVE.name());
        request.setStartTime(startTime);
        request.setEndTime(startTime.plusHours(8));
        request.setReason("年假");
        request.setStatus(FlowRequestStatus.PENDING.name());
        request.setCurrentApproverId(2L);
        request.setCreatedAt(startTime.minusDays(1));
        request.setUpdatedAt(startTime.minusDays(1));
        return request;
    }
}
