package com.example.KendyDigital.dto.inventory.response;

import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import java.time.Instant;

public record UserAccountCredentialResponse(
        Long id,
        Long serviceId,
        String serviceName,
        String orderCode,
        String loginIdentifier,
        String passwordSecret,
        String recoveryInfo,
        String twoFactorSecret,
        String usageNote,
        AccountCredentialStatus status,
        Instant deliveredAt,
        Instant expiresAt,
        Instant warrantyUntil) {

    public static UserAccountCredentialResponse from(AccountCredential credential) {
        return new UserAccountCredentialResponse(
                credential.getId(),
                credential.getService().getId(),
                credential.getService().getName(),
                credential.getAssignedOrder() == null ? null : credential.getAssignedOrder().getOrderCode(),
                credential.getLoginIdentifier(),
                credential.getPasswordSecret(),
                credential.getRecoveryInfo(),
                credential.getTwoFactorSecret(),
                credential.getUsageNote(),
                credential.getStatus(),
                credential.getDeliveredAt(),
                credential.getExpiresAt(),
                credential.getWarrantyUntil());
    }
}
