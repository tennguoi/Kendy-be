package com.example.KendyDigital.dto.monitoring.response;

import java.time.Instant;
import java.util.List;

public record ThreatOverviewResponse(
        int healthScore,
        long openAlerts,
        long criticalAlerts,
        long highAlerts,
        long bannedIps,
        long securityEvents24h,
        long loginFailures24h,
        long rateLimitEvents24h,
        long wafEvents24h,
        long webhookRejections24h,
        String topAttackType,
        List<SecurityAlertResponse> latestAlerts,
        List<SecurityTopIpResponse> topIps,
        Instant generatedAt) {
}
