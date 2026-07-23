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
import com.personaowl.oa.flow.mapper.FlowAttendanceMapper;
import com.personaowl.oa.flow.mapper.FlowRequestMapper;
import com.personaowl.oa.flow.mapper.FlowUserDirectoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlowApprovalServiceTest {
    @Mock
    private FlowRequestMapper requestMapper;
    @Mock
    private FlowActionLogMapper actionLogMapper;
    @Mock
    private FlowUserDirectoryMapper userDirectoryMapper;
    @Mock
    private FlowAttendanceMapper attendanceMapper;

    private FlowApprovalService service;
    private LocalDateTime startTime;

    @BeforeEach
    void setUp() {
        service = new FlowApprovalService(
                requestMapper, actionLogMapper, userDirectoryMapper, null, attendanceMapper);
        startTime = LocalDateTime.of(2026, 7, 23, 9, 0);
    }

    @Test
    void submitLeaveCreatesPendingRequestAndUsesDepartmentManager() {
        when(userDirectoryMapper.findDepartmentManager(1L)).thenReturn(2L);
        when(requestMapper.insert(any(FlowRequest.class))).thenAnswer(invocation -> {
            FlowRequest request = invocation.getArgument(0);
            request.setId(10L);
            return 1;
        });
        when(actionLogMapper.insert(any(FlowActionLog.class))).thenReturn(1);

        var response = service.submit(
                FlowRequestType.LEAVE,
                new FlowSubmitRequest(
                        startTime, startTime.plusHours(8), " 年假 ", "ANNUAL", null),
                1L);

        assertEquals(10L, response.id());
        assertEquals("LEAVE", response.requestType());
        assertEquals("PENDING", response.status());
        assertEquals("年假", response.reason());
        assertEquals("ANNUAL", response.leaveType());
        assertEquals(480, response.durationMinutes());
        assertEquals(2L, response.currentApproverId());
        verify(actionLogMapper).insert(any(FlowActionLog.class));
    }

    @Test
    void submitOvertimeUsesFallbackAdminAndCompensationOption() {
        when(userDirectoryMapper.findFallbackAdmin(1L)).thenReturn(3L);
        when(requestMapper.insert(any(FlowRequest.class))).thenAnswer(invocation -> {
            FlowRequest request = invocation.getArgument(0);
            request.setId(11L);
            return 1;
        });
        when(actionLogMapper.insert(any(FlowActionLog.class))).thenReturn(1);

        var response = service.submit(
                FlowRequestType.OVERTIME,
                new FlowSubmitRequest(
                        startTime, startTime.plusHours(2), "发布支持", null, "COMPENSATORY"),
                1L);

        assertEquals("OVERTIME", response.requestType());
        assertEquals("COMPENSATORY", response.overtimeCompensation());
        assertEquals(3L, response.currentApproverId());
    }

    @Test
    void submitRejectsInvalidTimeRange() {
        FlowSubmitRequest request = new FlowSubmitRequest(
                startTime, startTime.minusMinutes(1), "请假", "PERSONAL", null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submit(FlowRequestType.LEAVE, request, 1L));

        assertEquals(ErrorCode.INVALID_ARGUMENT, exception.errorCode());
    }

    @Test
    void submitRejectsOverlappingApplication() {
        when(requestMapper.countOverlapping(1L, startTime, startTime.plusHours(1))).thenReturn(1L);
        FlowSubmitRequest request = new FlowSubmitRequest(
                startTime, startTime.plusHours(1), "请假", "PERSONAL", null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submit(FlowRequestType.LEAVE, request, 1L));

        assertEquals("该时间段已有待审批或已通过的申请", exception.getMessage());
    }

    @Test
    void submitRejectsWhenNoApproverCanBeResolved() {
        FlowSubmitRequest request = new FlowSubmitRequest(
                startTime, startTime.plusHours(1), "加班", null, "PAY");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submit(FlowRequestType.OVERTIME, request, 1L));

        assertEquals("未找到可用审批人，请先配置部门负责人", exception.getMessage());
    }

    @Test
    void approveCompletesPendingLeaveWritesLogAndSynchronizesAttendance() {
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
        verify(attendanceMapper).insertApprovedLeave(any(Long.class), eq(1L), any(), any());
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
    void rejectRequiresComment() {
        when(requestMapper.selectById(10L)).thenReturn(pendingRequest());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.approve(
                        10L, new FlowApprovalRequest("REJECT", "  "), 2L));

        assertEquals("驳回申请时必须填写审批意见", exception.getMessage());
        verify(requestMapper, never()).completeApproval(any(), any(), any(), any());
    }

    @Test
    void applicantCanWithdrawPendingRequest() {
        when(requestMapper.selectById(10L)).thenReturn(pendingRequest());
        when(requestMapper.withdraw(eq(10L), eq(1L), any())).thenReturn(1);
        when(actionLogMapper.insert(any(FlowActionLog.class))).thenReturn(1);

        var response = service.withdraw(10L, 1L);

        assertEquals("WITHDRAWN", response.status());
        assertNull(response.currentApproverId());
        verify(requestMapper).withdraw(eq(10L), eq(1L), any());
    }

    @Test
    void detailReturnsCompleteTimeline() {
        when(requestMapper.selectById(10L)).thenReturn(pendingRequest());
        when(actionLogMapper.findTimeline(10L)).thenReturn(List.of());

        var response = service.detail(10L, 1L);

        assertEquals(10L, response.request().id());
        assertTrue(response.timeline().isEmpty());
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
        request.setLeaveType("ANNUAL");
        request.setDurationMinutes(480);
        request.setStatus(FlowRequestStatus.PENDING.name());
        request.setCurrentApproverId(2L);
        request.setCreatedAt(startTime.minusDays(1));
        request.setUpdatedAt(startTime.minusDays(1));
        return request;
    }
}
