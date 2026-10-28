package com.sft.transaction.dto;

import com.sft.category.dto.CategoryResponse;
import com.sft.transaction.CategorySource;
import com.sft.transaction.Transaction;
import com.sft.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        TransactionType type,
        String description,
        LocalDate occurredOn,
        CategoryResponse category,
        CategorySource categorySource,
        BigDecimal mlConfidence,
        Instant createdAt
) {
    /** Requiere la categoría ya cargada (ver @EntityGraph en TransactionRepository). */
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getType(),
                t.getDescription(),
                t.getOccurredOn(),
                t.getCategory() == null ? null : CategoryResponse.from(t.getCategory()),
                t.getCategorySource(),
                t.getMlConfidence(),
                t.getCreatedAt());
    }
}
