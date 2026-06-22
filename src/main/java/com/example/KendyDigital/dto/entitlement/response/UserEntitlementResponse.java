package com.example.KendyDigital.dto.entitlement.response;

import com.example.KendyDigital.model.catalog.AccessStrategy;
import com.example.KendyDigital.model.entitlement.EntitlementStatus;
import com.example.KendyDigital.model.entitlement.UserEntitlement;
import com.example.KendyDigital.model.inventory.AccountCredential;
import java.time.Instant;

public record UserEntitlementResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        Long serviceId,
        String serviceName,
        String orderCode,
        AccessStrategy accessStrategy,
        EntitlementStatus status,
        String accessIdentifier,
        String externalResourceId,
        Instant startsAt,
        Instant expiresAt,
        Instant renewalRequestedAt,
        String lastError,
        Long credentialId,
        String loginIdentifier,
        String passwordSecret,
        String recoveryInfo,
        String twoFactorSecret,
        String usageNote,
        Instant warrantyUntil) {

    public static UserEntitlementResponse from(UserEntitlement entitlement) {
        AccountCredential credential = entitlement.getSourceOrder().getDeliveredCredential();
        boolean revealCredential = entitlement.getAccessStrategy() == AccessStrategy.DEDICATED_ACCOUNT
                && (entitlement.getStatus() == EntitlementStatus.ACTIVE
                    || entitlement.getStatus() == EntitlementStatus.EXPIRING);
        return new UserEntitlementResponse(
                entitlement.getId(),
                entitlement.getUser().getId(),
                entitlement.getUser().getName(),
                entitlement.getUser().getEmail(),
                entitlement.getService().getId(),
                entitlement.getService().getName(),
                entitlement.getSourceOrder().getOrderCode(),
                entitlement.getAccessStrategy(),
                entitlement.getStatus(),
                entitlement.getAccessIdentifier(),
                entitlement.getExternalResourceId(),
                entitlement.getStartsAt(),
                entitlement.getExpiresAt(),
                entitlement.getRenewalRequestedAt(),
                entitlement.getLastError(),
                credential == null ? null : credential.getId(),
                revealCredential && credential != null ? credential.getLoginIdentifier() : null,
                revealCredential && credential != null ? credential.getPasswordSecret() : null,
                revealCredential && credential != null ? credential.getRecoveryInfo() : null,
                revealCredential && credential != null ? credential.getTwoFactorSecret() : null,
                credential == null ? null : credential.getUsageNote(),
                credential == null ? null : credential.getWarrantyUntil());
    }
}
