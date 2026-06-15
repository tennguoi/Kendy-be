package com.example.KendyDigital.dto.auth.response;

import com.example.KendyDigital.model.auth.AuthSession;
import java.time.Instant;

public record AuthSessionResponse(
        Long id,
        Long userId,
        Instant expiresAt,
        Instant revokedAt,
        Instant lastUsedAt,
        Instant createdAt) {
    public static AuthSessionResponse from(AuthSession session) {
        return new AuthSessionResponse(
                session.getId(),
                session.getUser().getId(),
                session.getExpiresAt(),
                session.getRevokedAt(),
                session.getLastUsedAt(),
                session.getCreatedAt());
    }
}
