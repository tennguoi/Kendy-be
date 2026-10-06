package com.example.KendyDigital.dto.monitoring.response;

import com.example.KendyDigital.model.auth.AuthSession;
import java.time.Instant;

public record SecuritySessionResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String userRole,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt,
        String createdIp,
        String lastIp,
        String country,
        String userAgent) {
    public static SecuritySessionResponse from(AuthSession session) {
        return new SecuritySessionResponse(
                session.getId(),
                session.getUser().getId(),
                session.getUser().getName(),
                session.getUser().getEmail(),
                session.getUser().getRole() == null ? null : session.getUser().getRole().name(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getExpiresAt(),
                session.getCreatedIp(),
                session.getLastIp(),
                session.getCountry(),
                session.getUserAgent());
    }
}
