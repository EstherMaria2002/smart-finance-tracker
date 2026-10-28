package com.sft.report.dto;

import com.sft.transaction.TransactionType;

import java.math.BigDecimal;

/** Fila cruda de la consulta agregada por mes y tipo (uso interno). */
public record MonthlyTotalRow(Integer month, TransactionType type, BigDecimal total) {
}
