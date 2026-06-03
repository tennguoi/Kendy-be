package com.example.KendyDigital.dto;

import java.time.Instant;

public record SecurityTokenResponse(
        String message,
        Instant expiresAt,
        String token) {
}
