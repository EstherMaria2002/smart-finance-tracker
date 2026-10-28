package com.sft.config;

import com.sft.ml.MlClient;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/** WebClient preconfigurado hacia el ML service (URL base, API key y timeouts). */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient mlWebClient(MlProperties props) {
        HttpClient httpClient = HttpClient.create()
                // Timeouts a nivel de red; MlClient añade además un timeout reactivo total.
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.timeoutMs())
                .responseTimeout(Duration.ofMillis(props.timeoutMs()));

        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // Cabecera interna: SIEMPRE "X-Internal-Api-Key" (con L), igual en el ML service.
                .defaultHeader(MlClient.API_KEY_HEADER, props.apiKey() == null ? "" : props.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
