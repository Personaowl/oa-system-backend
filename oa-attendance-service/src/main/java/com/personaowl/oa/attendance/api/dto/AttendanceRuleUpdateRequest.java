package com.personaowl.oa.attendance.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record AttendanceRuleUpdateRequest(
        @NotNull(message = "上班时间不能为空") LocalTime workStart,
        @NotNull(message = "下班时间不能为空") LocalTime workEnd,
        @Min(value = 0, message = "迟到宽限时间不能小于0分钟")
        @Max(value = 180, message = "迟到宽限时间不能超过180分钟")
        int lateThresholdMinutes
) {
}
