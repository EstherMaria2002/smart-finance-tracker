package com.sft.report.dto;

import java.math.BigDecimal;

/** Ingresos y gastos de un mes (1-12). */
public record MonthlyReportItem(int month, BigDecimal income, BigDecimal expense) {
}
