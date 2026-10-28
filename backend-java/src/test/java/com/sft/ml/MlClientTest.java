package com.sft.ml;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.sft.common.exception.MlServiceException;
import com.sft.config.MlProperties;
import com.sft.config.WebClientConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba MlClient contra un ML service simulado con WireMock (sin Spring, rápido). */
class MlClientTest {

    private static WireMockServer wireMock;
    private MlClient client;

    @BeforeAll
    static void startServer() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopServer() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        MlProperties props = new MlProperties(wireMock.baseUrl(), "test-key", 800, 0.30);
        // Registro nuevo por test: un circuito abierto en un test no afecta a los demás.
        client = new MlClient(new WebClientConfig().mlWebClient(props), props, CircuitBreakerRegistry.ofDefaults());
    }

    @Test
    void sendsInternalApiKeyHeader_andParsesResponse() {
        wireMock.stubFor(post(urlEqualTo("/predict"))
                .willReturn(okJson("{\"category\":\"Restaurantes\",\"confidence\":0.87,\"source\":\"ML\"}")));

        MlCategoryResponse res = client.predict("Cena en restaurante italiano", new BigDecimal("42.50"));

        assertThat(res.category()).isEqualTo("Restaurantes");
        assertThat(res.confidence()).isEqualTo(0.87);
        assertThat(res.isRealPrediction()).isTrue();
        wireMock.verify(postRequestedFor(urlEqualTo("/predict"))
                .withHeader("X-Internal-Api-Key", equalTo("test-key")));
    }

    @Test
    void timesOutAfter800ms_andThrowsMlServiceException() {
        wireMock.stubFor(post(urlEqualTo("/predict"))
                .willReturn(okJson("{\"category\":\"Ocio\",\"confidence\":0.9,\"source\":\"ML\"}")
                        .withFixedDelay(2000)));

        long start = System.nanoTime();
        assertThatThrownBy(() -> client.predict("Cine", BigDecimal.TEN))
                .isInstanceOf(MlServiceException.class);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(1500);   // cortó cerca de 800 ms, no esperó los 2000
    }

    @Test
    void serverError_throwsMlServiceException() {
        wireMock.stubFor(post(urlEqualTo("/predict")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.predict("Cine", BigDecimal.TEN)).isInstanceOf(MlServiceException.class);
    }

    @Test
    void wrongApiKey_403_throwsMlServiceException() {
        wireMock.stubFor(post(urlEqualTo("/predict")).willReturn(aResponse().withStatus(403)));

        assertThatThrownBy(() -> client.predict("Cine", BigDecimal.TEN)).isInstanceOf(MlServiceException.class);
    }

    @Test
    void connectionRefused_throwsMlServiceException() {
        MlProperties props = new MlProperties("http://localhost:1", "test-key", 800, 0.30);
        MlClient deadClient = new MlClient(new WebClientConfig().mlWebClient(props), props,
                CircuitBreakerRegistry.ofDefaults());

        assertThatThrownBy(() -> deadClient.predict("Cine", BigDecimal.TEN)).isInstanceOf(MlServiceException.class);
    }

    @Test
    void emptyBody_throwsMlServiceException() {
        wireMock.stubFor(post(urlEqualTo("/predict")).willReturn(okJson("{}")));

        assertThatThrownBy(() -> client.predict("Cine", BigDecimal.TEN)).isInstanceOf(MlServiceException.class);
    }
}
