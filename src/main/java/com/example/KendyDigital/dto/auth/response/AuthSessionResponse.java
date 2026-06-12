package com.example.KendyDigital.dto.auth.response;

import java.time.Instant;

import com.example.KendyDigital.model.AuthSession;

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
