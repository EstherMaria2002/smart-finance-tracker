package com.sft.transaction;

/** Quién decidió la categoría de una transacción (trazabilidad). */
public enum CategorySource {
    /** Predicción del modelo de ML. */
    ML,
    /** Elegida explícitamente por el usuario. */
    MANUAL,
    /** Reglas por palabra clave del backend (fallback). */
    RULE
}
