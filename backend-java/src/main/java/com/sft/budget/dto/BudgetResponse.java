package com.sft.budget.dto;

import com.sft.budget.Budget;
import com.sft.category.dto.CategoryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

/** spent = gasto (EXPENSE) acumulado de esa categoría en ese mes. */
public record BudgetResponse(Long id, CategoryResponse category, LocalDate month,
                             BigDecimal limitAmount, BigDecimal spent) {

    public static BudgetResponse from(Budget b, BigDecimal spent) {
        return new BudgetResponse(b.getId(), CategoryResponse.from(b.getCategory()), b.getMonth(),
                b.getLimitAmount(), spent);
    }
}
