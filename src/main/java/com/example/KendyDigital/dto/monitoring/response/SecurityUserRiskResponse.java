package com.example.KendyDigital.dto.monitoring.response;

import java.time.Instant;
import java.util.List;

public record SecurityUserRiskResponse(
        Long userId,
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
