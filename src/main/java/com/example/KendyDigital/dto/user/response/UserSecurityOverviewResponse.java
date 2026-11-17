package com.example.KendyDigital.dto.user.response;

import java.time.Instant;
import java.util.UUID;

public record UserSecurityOverviewResponse(
        UUID userId,
        boolean emailVerified,
        Instant emailVerifiedAt,
        boolean twoFactorEnabled,
        Instant passwordChangedAt,
        long activeSessions,
        long activeApiKeys,
        boolean hasPassword,
        String oauthProvider) {
}
