package com.personaowl.oa.user.api.dto;

import java.math.BigDecimal;

public record SalaryGradeResponse(
        String code,
        BigDecimal baseSalary
) {
}
