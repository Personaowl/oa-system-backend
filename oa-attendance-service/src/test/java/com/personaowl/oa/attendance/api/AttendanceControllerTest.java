package com.personaowl.oa.attendance.api;

import com.personaowl.oa.attendance.api.dto.CheckInResponse;
import com.personaowl.oa.attendance.api.dto.CheckOutResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRecordItemResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRecordPageResponse;
import com.personaowl.oa.attendance.api.dto.AttendanceRecordQuery;
import com.personaowl.oa.attendance.api.dto.TodayStatusResponse;
import com.personaowl.oa.attendance.application.AttendanceApplicationService;
import com.personaowl.oa.attendance.domain.AttendanceStatus;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.common.web.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AttendanceApplicationService attendanceService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(new AttendanceController(attendanceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void returnsStandardResponseWithStringRecordId() throws Exception {
        when(attendanceService.checkIn(any())).thenReturn(new CheckInResponse(
                "1946539000000000001",
                LocalDate.of(2026, 7, 21),
                OffsetDateTime.parse("2026-07-21T09:03:12+08:00"),
                AttendanceStatus.IN_PROGRESS,
                false,
                0));

        mockMvc.perform(post("/api/v1/attendance/check-in")
                        .header("X-User-Id", "10001")
                        .header("X-Permissions", "attendance:record:query")
                        .header("X-Trace-Id", "trace-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.traceId").value("trace-1"))
                .andExpect(jsonPath("$.data.recordId").value("1946539000000000001"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.late").value(false));
    }

    @Test
    void returnsAttendanceDuplicateError() throws Exception {
        when(attendanceService.checkIn(any()))
                .thenThrow(new BusinessException(ErrorCode.ATTENDANCE_ALREADY_CHECKED_IN));

        mockMvc.perform(post("/api/v1/attendance/check-in")
                        .header("X-User-Id", "10001")
                        .header("X-Trace-Id", "trace-2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B0110"))
                .andExpect(jsonPath("$.message").value("今日已完成上班打卡"));
    }

    @Test
    void rejectsRequestWithoutTrustedUserHeader() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/check-in")
                        .header("X-Trace-Id", "trace-3"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A0103"))
                .andExpect(jsonPath("$.message").value("缺少可信用户身份"));

        verifyNoInteractions(attendanceService);
    }

    @Test
    void returnsTodayStatusWithOffsetTimeAndActions() throws Exception {
        when(attendanceService.getTodayStatus(any())).thenReturn(new TodayStatusResponse(
                LocalDate.of(2026, 7, 21),
                OffsetDateTime.parse("2026-07-21T09:03:12+08:00"),
                null,
                AttendanceStatus.IN_PROGRESS,
                false,
                true));

        mockMvc.perform(get("/api/v1/attendance/today")
                        .header("X-User-Id", "10001")
                        .header("X-Trace-Id", "trace-today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.traceId").value("trace-today"))
                .andExpect(jsonPath("$.data.workDate").value("2026-07-21"))
                .andExpect(jsonPath("$.data.checkInTime").value("2026-07-21T09:03:12+08:00"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.canCheckIn").value(false))
                .andExpect(jsonPath("$.data.canCheckOut").value(true));
    }

    @Test
    void returnsSuccessfulTodayDataWhenNoRecordExists() throws Exception {
        when(attendanceService.getTodayStatus(any())).thenReturn(new TodayStatusResponse(
                LocalDate.of(2026, 7, 21), null, null, null, true, false));

        mockMvc.perform(get("/api/v1/attendance/today")
                        .header("X-User-Id", "10002")
                        .header("X-Trace-Id", "trace-empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.workDate").value("2026-07-21"))
                .andExpect(jsonPath("$.data.checkInTime").doesNotExist())
                .andExpect(jsonPath("$.data.checkOutTime").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.canCheckIn").value(true))
                .andExpect(jsonPath("$.data.canCheckOut").value(false));
    }

    @Test
    void rejectsTodayRequestWithoutTrustedUserHeader() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/today")
                        .header("X-Trace-Id", "trace-no-user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A0103"));

        verifyNoInteractions(attendanceService);
    }

    @Test
    void returnsStandardCheckOutResponse() throws Exception {
        when(attendanceService.checkOut(any())).thenReturn(new CheckOutResponse(
                "1946539000000000001",
                OffsetDateTime.parse("2026-07-21T18:05:00+08:00"),
                AttendanceStatus.NORMAL,
                false,
                0));

        mockMvc.perform(post("/api/v1/attendance/check-out")
                        .header("X-User-Id", "10001")
                        .header("X-Trace-Id", "trace-check-out"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.traceId").value("trace-check-out"))
                .andExpect(jsonPath("$.data.recordId").value("1946539000000000001"))
                .andExpect(jsonPath("$.data.checkOutTime").value("2026-07-21T18:05:00+08:00"))
                .andExpect(jsonPath("$.data.status").value("NORMAL"))
                .andExpect(jsonPath("$.data.earlyLeave").value(false))
                .andExpect(jsonPath("$.data.earlyLeaveMinutes").value(0));
    }

    @Test
    void returnsAlreadyCheckedOutError() throws Exception {
        when(attendanceService.checkOut(any()))
                .thenThrow(new BusinessException(ErrorCode.ATTENDANCE_ALREADY_CHECKED_OUT));

        mockMvc.perform(post("/api/v1/attendance/check-out")
                        .header("X-User-Id", "10001")
                        .header("X-Trace-Id", "trace-checked-out"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B0111"))
                .andExpect(jsonPath("$.message").value("今日已完成下班打卡"));
    }

    @Test
    void returnsCheckInRequiredError() throws Exception {
        when(attendanceService.checkOut(any()))
                .thenThrow(new BusinessException(ErrorCode.ATTENDANCE_CHECK_IN_REQUIRED));

        mockMvc.perform(post("/api/v1/attendance/check-out")
                        .header("X-User-Id", "10002")
                        .header("X-Trace-Id", "trace-no-check-in"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B0112"))
                .andExpect(jsonPath("$.message").value("请先完成上班打卡"));
    }

    @Test
    void rejectsCheckOutWithoutTrustedUserHeader() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/check-out")
                        .header("X-Trace-Id", "trace-check-out-no-user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A0103"));

        verifyNoInteractions(attendanceService);
    }

    @Test
    void returnsStandardPagedAttendanceRecords() throws Exception {
        AttendanceRecordItemResponse item = new AttendanceRecordItemResponse(
                "1946539000000000001",
                "10001",
                LocalDate.of(2026, 7, 21),
                OffsetDateTime.parse("2026-07-21T09:03:00+08:00"),
                OffsetDateTime.parse("2026-07-21T18:05:00+08:00"),
                AttendanceStatus.NORMAL,
                0,
                0);
        when(attendanceService.getRecords(any(), any())).thenReturn(new AttendanceRecordPageResponse(
                List.of(item), 1, 10, 1, "SELF", false, "仅查询当前用户本人记录"));

        mockMvc.perform(get("/api/v1/attendance/records")
                        .header("X-User-Id", "10001")
                        .header("X-Trace-Id", "trace-records")
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31")
                        .param("status", "NORMAL")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.traceId").value("trace-records"))
                .andExpect(jsonPath("$.data.items[0].recordId").value("1946539000000000001"))
                .andExpect(jsonPath("$.data.items[0].userId").value("10001"))
                .andExpect(jsonPath("$.data.items[0].status").value("NORMAL"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.dataScope").value("SELF"))
                .andExpect(jsonPath("$.data.departmentFilterApplied").value(false));

        ArgumentCaptor<AttendanceRecordQuery> queryCaptor = ArgumentCaptor.forClass(AttendanceRecordQuery.class);
        verify(attendanceService).getRecords(any(), queryCaptor.capture());
        assertEquals(LocalDate.of(2026, 7, 1), queryCaptor.getValue().startDate());
        assertEquals(LocalDate.of(2026, 7, 31), queryCaptor.getValue().endDate());
        assertEquals(AttendanceStatus.NORMAL, queryCaptor.getValue().status());
        assertEquals(10, queryCaptor.getValue().size());
    }

    @Test
    void returnsForbiddenForUnauthorizedCrossUserRecordQuery() throws Exception {
        when(attendanceService.getRecords(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/v1/attendance/records")
                        .header("X-User-Id", "10001")
                        .param("userId", "10002"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    void returnsInvalidArgumentForMalformedRecordDate() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/records")
                        .header("X-User-Id", "10001")
                        .param("startDate", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B0101"))
                .andExpect(jsonPath("$.message").value("startDate: 参数格式不正确"));

        verifyNoInteractions(attendanceService);
    }

    @Test
    void rejectsRecordQueryWithoutTrustedUserHeader() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/records")
                        .header("X-Trace-Id", "trace-records-no-user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A0103"));

        verifyNoInteractions(attendanceService);
    }
}
