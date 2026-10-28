package com.sft.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Respuesta de POST /predict: { "category": "Restaurantes", "confidence": 0.87, "source": "ML" }.
 * source = "RULE" y confidence = 0.0 significa "el ML no tiene modelo": no es una predicción real.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MlCategoryResponse(String category, double confidence, String source) {

    public boolean isRealPrediction() {
        return "ML".equalsIgnoreCase(source);
    }
}
