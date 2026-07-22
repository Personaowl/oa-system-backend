package com.personaowl.oa.attendance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.personaowl.oa.attendance.api.dto.MonthlyStatisticsResponse;
import com.personaowl.oa.attendance.api.dto.StatisticsSummaryResponse;
import com.personaowl.oa.attendance.application.AttendanceStatisticsService;
import com.personaowl.oa.common.core.error.BusinessException;
import com.personaowl.oa.common.core.error.ErrorCode;
import com.personaowl.oa.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttendanceStatisticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AttendanceStatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(new AttendanceStatisticsController(statisticsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void returnsMonthlyStatisticsInStandardEnvelope() throws Exception {
        when(statisticsService.getMonthlyStatistics(any(), any())).thenReturn(new MonthlyStatisticsResponse(
                "10001", YearMonth.of(2026, 7),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                5, 2, 3, 2, 1));

        mockMvc.perform(get("/api/v1/attendance/statistics/monthly")
                        .header("X-User-Id", "10001")
                        .header("X-Trace-Id", "trace-monthly")
                        .param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.traceId").value("trace-monthly"))
                .andExpect(jsonPath("$.data.userId").value("10001"))
                .andExpect(jsonPath("$.data.month").value("2026-07"))
                .andExpect(jsonPath("$.data.totalRecords").value(5))
                .andExpect(jsonPath("$.data.lateCount").value(3))
                .andExpect(jsonPath("$.data.missingCheckOutCount").value(1));
    }

    @Test
    void returnsAuthorizedAdministrativeSummary() throws Exception {
        when(statisticsService.getSummary(any(), any(), any(), anyLong()))
                .thenReturn(new StatisticsSummaryResponse(
                        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                        10L, false, "departmentId 本期仅预留，未参与数据过滤",
                        8, 4, 2, 3, 2, 1));

        mockMvc.perform(get("/api/v1/attendance/statistics/summary")
                        .header("X-User-Id", "90001")
                        .header("X-Permissions", "attendance:statistics:query")
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31")
                        .param("departmentId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalRecords").value(8))
                .andExpect(jsonPath("$.data.totalUsers").value(4))
                .andExpect(jsonPath("$.data.departmentFilterApplied").value(false))
                .andExpect(jsonPath("$.data.scopeNote")
                        .value("departmentId 本期仅预留，未参与数据过滤"));
    }

    @Test
    void returnsForbiddenWhenSummaryPermissionIsMissing() throws Exception {
        when(statisticsService.getSummary(any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/v1/attendance/statistics/summary")
                        .header("X-User-Id", "10001")
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A0201"));
    }

    @Test
    void returnsInvalidArgumentForMalformedMonth() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/statistics/monthly")
                        .header("X-User-Id", "10001")
                        .param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("B0101"));

        verifyNoInteractions(statisticsService);
    }

    @Test
    void rejectsStatisticsRequestWithoutTrustedUser() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/statistics/monthly")
                        .header("X-Trace-Id", "trace-no-user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A0103"));

        verifyNoInteractions(statisticsService);
    }
}
