package com.sft.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades "sft.ml.*"
 *
 * @param baseUrl       URL interna del ML service (p. ej. http://ml-service:8000)
 * @param apiKey        valor de la cabecera X-Internal-Api-Key
 * @param timeoutMs     timeout total de la llamada (800 ms por diseño)
 * @param minConfidence por debajo de este umbral se prefieren las reglas por palabra clave
 */
@ConfigurationProperties(prefix = "sft.ml")
public record MlProperties(String baseUrl, String apiKey, int timeoutMs, double minConfidence) {
}
