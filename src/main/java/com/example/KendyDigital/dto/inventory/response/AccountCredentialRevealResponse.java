package com.example.KendyDigital.dto.inventory.response;

import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import java.time.Instant;

public record AccountCredentialRevealResponse(
        Long id,
        Long serviceId,
        String serviceName,
        String loginIdentifier,
        String passwordSecret,
        String recoveryInfo,
        String twoFactorSecret,
        String usageNote,
        AccountCredentialStatus status,
        String assignedOrderCode,
        String deliveredToEmail,
        Instant deliveredAt,
        Instant expiresAt,
        Instant warrantyUntil) {
    public static AccountCredentialRevealResponse from(AccountCredential credential) {
        return new AccountCredentialRevealResponse(
                credential.getId(),
                credential.getService().getId(),
                credential.getService().getName(),
                credential.getLoginIdentifier(),
                credential.getPasswordSecret(),
                credential.getRecoveryInfo(),
                credential.getTwoFactorSecret(),
                credential.getUsageNote(),
                credential.getStatus(),
                credential.getAssignedOrder() == null ? null : credential.getAssignedOrder().getOrderCode(),
                credential.getDeliveredToUser() == null ? null : credential.getDeliveredToUser().getEmail(),
                credential.getDeliveredAt(),
                credential.getExpiresAt(),
                credential.getWarrantyUntil());
    }
}
