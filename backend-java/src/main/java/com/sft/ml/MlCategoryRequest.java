package com.sft.ml;

import java.math.BigDecimal;

/** Cuerpo de POST /predict del ML service. */
public record MlCategoryRequest(String description, BigDecimal amount) {
}
