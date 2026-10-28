package com.sft.ml;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MlFallbackServiceTest {

    private final MlFallbackService service = new MlFallbackService();

    @Test
    void restaurantKeywords() {
        assertThat(service.categorize("Cena en restaurante italiano")).isEqualTo("Restaurantes");
        assertThat(service.categorize("CENAMOS con amigos")).isEqualTo("Restaurantes");
    }

    @Test
    void transportKeywords() {
        assertThat(service.categorize("Uber al aeropuerto")).isEqualTo("Transporte");
        assertThat(service.categorize("Taxi nocturno")).isEqualTo("Transporte");
    }

    @Test
    void uberEatsIsFoodNotTransport() {
        assertThat(service.categorize("Pedido Uber Eats")).isEqualTo("Restaurantes");
    }

    @Test
    void ignoresAccentsAndCase() {
        assertThat(service.categorize("NÓMINA de septiembre")).isEqualTo("Nómina");
    }

    @Test
    void doesNotMatchInsideOtherWords() {
        // "bar" no debe coincidir con "barato"; "agua" no debe coincidir con "aguacate".
        assertThat(service.categorize("Compré algo barato")).isEqualTo("Otros");
        assertThat(service.categorize("Aguacate")).isEqualTo("Otros");
    }

    @Test
    void unknownOrEmptyDefaultsToOtros() {
        assertThat(service.categorize("xyz 123")).isEqualTo("Otros");
        assertThat(service.categorize("")).isEqualTo("Otros");
        assertThat(service.categorize(null)).isEqualTo("Otros");
    }
}
