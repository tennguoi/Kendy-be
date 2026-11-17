package com.example.KendyDigital.dto.auth.response;

import com.example.KendyDigital.model.auth.AuthSession;
import java.time.Instant;
import java.util.UUID;

public record AuthSessionResponse(
        UUID id,
        UUID userId,
        Instant expiresAt,
        Instant revokedAt,
        Instant lastUsedAt,
        Instant createdAt) {
    public static AuthSessionResponse from(AuthSession session) {
        return new AuthSessionResponse(
                session.getPublicId(),
                session.getUser().getPublicId(),
                session.getExpiresAt(),
                session.getRevokedAt(),
                session.getLastUsedAt(),
                session.getCreatedAt());
    }
}
