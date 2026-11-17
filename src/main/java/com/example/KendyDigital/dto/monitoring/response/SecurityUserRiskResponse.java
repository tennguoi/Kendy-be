package com.example.KendyDigital.dto.monitoring.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SecurityUserRiskResponse(
        UUID userId,
        String name,
        String email,
        String role,
        double riskScore,
        int failedLoginAttempts,
        boolean locked,
        boolean twoFactorEnabled,
        boolean walletFrozen,
        Instant lastLoginAt,
        String lastLoginIp,
        String lastLoginCountry,
        long securityEvents24h,
        List<SecurityEventResponse> recentEvents) {
}
