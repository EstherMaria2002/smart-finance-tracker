package com.sft.transaction.dto;

import com.sft.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** PATCH: solo se modifican los campos presentes (no nulos). Un categoryId => categoría MANUAL. */
public record TransactionUpdateRequest(
        @DecimalMin(value = "0.01", message = "El importe debe ser mayor que 0")
        @Digits(integer = 12, fraction = 2) BigDecimal amount,
        TransactionType type,
        @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "La descripción no puede estar vacía") String description,
        LocalDate occurredOn,
        Long categoryId
) {
}
