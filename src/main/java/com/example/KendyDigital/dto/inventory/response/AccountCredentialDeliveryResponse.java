package com.example.KendyDigital.dto.inventory.response;

import com.example.KendyDigital.model.inventory.AccountCredential;
import java.time.Instant;

public record AccountCredentialDeliveryResponse(
        Long credentialId,
        String loginIdentifier,
        String passwordSecret,
        String recoveryInfo,
        String twoFactorSecret,
        String usageNote,
        Instant deliveredAt,
        Instant expiresAt,
        Instant warrantyUntil) {
    public static AccountCredentialDeliveryResponse from(AccountCredential credential) {
        if (credential == null) {
            return null;
        }
        return new AccountCredentialDeliveryResponse(
                credential.getId(),
                credential.getLoginIdentifier(),
                credential.getPasswordSecret(),
                credential.getRecoveryInfo(),
                credential.getTwoFactorSecret(),
                credential.getUsageNote(),
                credential.getDeliveredAt(),
                credential.getExpiresAt(),
                credential.getWarrantyUntil());
    }
}
