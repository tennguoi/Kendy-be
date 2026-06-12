package com.example.KendyDigital.dto.auth.response;

import java.time.Instant;

public record SecurityTokenResponse(
        String message,
        Instant expiresAt,
        String token) {
}
