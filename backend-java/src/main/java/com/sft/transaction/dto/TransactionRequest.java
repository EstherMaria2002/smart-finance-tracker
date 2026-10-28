package com.sft.transaction.dto;

import com.sft.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * categoryId == null  => el backend categoriza solo (ML, y reglas si el ML falla).
 * categoryId != null  => categoría manual del usuario.
 */
public record TransactionRequest(
        @NotNull @DecimalMin(value = "0.01", message = "El importe debe ser mayor que 0")
        @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull TransactionType type,
        @NotBlank @Size(max = 255) String description,
        @NotNull LocalDate occurredOn,
        Long categoryId
) {
}
