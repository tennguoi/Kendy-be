package com.example.KendyDigital.dto.monitoring.response;

import java.time.Instant;

/**
 * Aggregated "security posture" snapshot for the admin security monitor.
 */
public record SecurityOverviewResponse(
        long lockedAccounts,
        long accountsWithFailedLogins,
        long activeSessions,
        long activeApiKeys,
        long adminsWithout2fa,
        long totalAdmins,
        long webhookRejections24h,
        long credentialReveals24h,
        long sensitiveChanges24h,
        Instant generatedAt) {
}
