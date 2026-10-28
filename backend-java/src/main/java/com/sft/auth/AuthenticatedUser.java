package com.sft.auth;

/** Principal que JwtAuthFilter deja en el SecurityContext (se inyecta con @AuthenticationPrincipal). */
public record AuthenticatedUser(Long id, String email) {
}
