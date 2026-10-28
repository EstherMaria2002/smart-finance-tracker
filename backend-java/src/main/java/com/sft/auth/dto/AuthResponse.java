package com.sft.auth.dto;

import com.sft.user.UserResponse;

public record AuthResponse(String token, String tokenType, long expiresIn, UserResponse user) {

    public static AuthResponse bearer(String token, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
