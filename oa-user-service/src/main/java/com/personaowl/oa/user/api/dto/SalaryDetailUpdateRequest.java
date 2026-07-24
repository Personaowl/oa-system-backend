package com.personaowl.oa.user.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record SalaryDetailUpdateRequest(
        @NotBlank
        @Pattern(regexp = "^(1[3-9]|20)[ABC]$", message = "职级必须在13A至20C之间")
        String salaryGrade,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal performanceSalary,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal deductionSalary
) {
}
