package com.sft.ml;

import com.sft.common.exception.MlServiceException;
import com.sft.config.MlProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Cliente HTTP hacia el ML service (FastAPI, red interna).
 *
 * Garantías:
 *  - Cabecera X-Internal-Api-Key en cada petición (la añade WebClientConfig).
 *  - Timeout duro de {@code sft.ml.timeout-ms} (800 ms).
 *  - Circuit breaker (Resilience4j "mlService"): tras varios fallos seguidos deja de intentarlo
 *    durante un rato y falla al instante, para no encadenar 800 ms de espera en cada transacción.
 *  - TODO fallo (timeout, conexión rechazada, 4xx/5xx, JSON raro, circuito abierto) se normaliza a
 *    {@link MlServiceException}. Quien llama decide el fallback; aquí nunca se resuelve.
 */
@Component
public class MlClient {

    /** Siempre "X-Internal-Api-Key" (con L). Debe coincidir con el ML service. */
    public static final String API_KEY_HEADER = "X-Internal-Api-Key";
    static final String CIRCUIT_BREAKER_NAME = "mlService";

    private static final Logger log = LoggerFactory.getLogger(MlClient.class);

    private final WebClient webClient;
    private final MlProperties props;
    private final CircuitBreaker circuitBreaker;

    public MlClient(@Qualifier("mlWebClient") WebClient mlWebClient,
                    MlProperties props,
                    CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = mlWebClient;
        this.props = props;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            log.warn("ML_INTERNAL_API_KEY vacío: el ML service rechazará las llamadas y se usará siempre el fallback");
        }
    }

    /**
     * Pide al ML service la categoría de una transacción.
     *
     * @throws MlServiceException ante cualquier fallo. Nunca devuelve null.
     */
    public MlCategoryResponse predict(String description, BigDecimal amount) {
        Duration timeout = Duration.ofMillis(props.timeoutMs());
        try {
            MlCategoryResponse response = webClient.post()
                    .uri("/predict")
                    .bodyValue(new MlCategoryRequest(description, amount))
                    .retrieve()                                   // 4xx/5xx -> WebClientResponseException
                    .bodyToMono(MlCategoryResponse.class)
                    .timeout(timeout)                             // 800 ms -> TimeoutException
                    // El breaker envuelve al timeout: un timeout cuenta como fallo.
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    // Red de seguridad por si el timeout reactivo no saltara.
                    .block(timeout.plusMillis(200));

            if (response == null || response.category() == null || response.category().isBlank()) {
                throw new MlServiceException("Respuesta vacía del ML service");
            }
            return response;
        } catch (MlServiceException e) {
            throw e;
        } catch (Exception e) {
            // Timeout, conexión, 403 por API key errónea, circuito abierto (CallNotPermittedException)...
            throw new MlServiceException(
                    "ML service no disponible (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")", e);
        }
    }
}
