package com.sft.auth;

import com.sft.auth.dto.AuthResponse;
import com.sft.auth.dto.LoginRequest;
import com.sft.auth.dto.RegisterRequest;
import com.sft.common.exception.InvalidCredentialsException;
import com.sft.user.User;
import com.sft.user.UserResponse;
import com.sft.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        User user = userService.create(req.email(), passwordEncoder.encode(req.password()), req.displayName());
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        // Mismo error si el email no existe o la contraseña falla: no revela qué cuentas existen.
        User user = userService.findByEmail(req.email())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return AuthResponse.bearer(jwtService.generateToken(user), jwtService.expiresInSeconds(),
                UserResponse.from(user));
    }
}
