package com.sft.auth;

import com.sft.config.JwtProperties;
import com.sft.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Emite y valida JWT (HS256). El token EXPIRA tras sft.jwt.expiration-minutes. */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(JwtProperties props) {
        byte[] secret = props.secret() == null ? new byte[0] : props.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            // HS256 exige >= 256 bits. Falla al arrancar con un mensaje claro en vez de en el primer login.
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 caracteres (genera uno con: openssl rand -hex 32)");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.ttl = Duration.ofMinutes(props.expirationMinutes());
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** @throws JwtException si el token está caducado, manipulado o mal formado. */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(Long.valueOf(claims.getSubject()), claims.get("email", String.class));
    }

    public long expiresInSeconds() {
        return ttl.toSeconds();
    }
}
