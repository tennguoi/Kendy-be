package com.example.KendyDigital.dto.auth.response;

import java.time.Instant;

public record AuthTokenResponse(
        String accessToken,
        Instant expiresAt,
        AuthUserResponse user) {
}
