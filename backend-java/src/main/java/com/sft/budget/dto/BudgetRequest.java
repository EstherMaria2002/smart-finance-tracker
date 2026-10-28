package com.sft.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** month: cualquier día del mes; se normaliza al día 1. */
public record BudgetRequest(
        @NotNull Long categoryId,
        @NotNull LocalDate month,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal limitAmount
) {
}
