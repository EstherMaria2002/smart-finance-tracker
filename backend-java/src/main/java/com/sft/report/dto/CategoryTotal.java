package com.sft.report.dto;

import java.math.BigDecimal;

/** Total por categoría. categoryId/name pueden ser null para transacciones sin categoría. */
public record CategoryTotal(Long categoryId, String name, String color, BigDecimal total) {
}
