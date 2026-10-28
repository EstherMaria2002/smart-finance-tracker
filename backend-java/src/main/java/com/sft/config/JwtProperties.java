package com.sft.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades "sft.jwt.*" */
@ConfigurationProperties(prefix = "sft.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
