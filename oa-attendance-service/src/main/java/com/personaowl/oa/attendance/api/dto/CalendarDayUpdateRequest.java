package com.personaowl.oa.attendance.api.dto;

import com.personaowl.oa.attendance.domain.CalendarDayType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CalendarDayUpdateRequest(
        @NotNull CalendarDayType dayType,
        @Size(max = 100) String holidayName
) {
}
