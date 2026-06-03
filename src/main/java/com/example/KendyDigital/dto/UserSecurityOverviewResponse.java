package com.example.KendyDigital.dto;

import java.time.Instant;

public record UserSecurityOverviewResponse(
        Long userId,
        boolean emailVerified,
        Instant emailVerifiedAt,
        boolean twoFactorEnabled,
        Instant passwordChangedAt,
        long activeSessions,
        long activeApiKeys) {
}
