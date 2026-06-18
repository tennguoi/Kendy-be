package com.example.KendyDigital.dto.inventory.response;

import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import java.time.Instant;

public record AccountCredentialAdminResponse(
        Long id,
        Long serviceId,
        String serviceName,
        String loginIdentifier,
        String passwordSecret,
        String recoveryInfo,
        String twoFactorSecret,
        String usageNote,
        String internalNote,
        AccountCredentialStatus status,
        Long assignedOrderId,
        String assignedOrderCode,
        Long reservedCheckoutId,
        String reservedCheckoutCode,
        Long reservedByUserId,
        Long deliveredToUserId,
        String deliveredToName,
        String deliveredToEmail,
        String deliveredToPhone,
        Instant reservedAt,
        Instant reservedUntil,
        Instant deliveredAt,
        Instant expiresAt,
        Instant warrantyUntil,
        Instant createdAt) {
    public static AccountCredentialAdminResponse from(AccountCredential credential) {
        return new AccountCredentialAdminResponse(
                credential.getId(),
                credential.getService().getId(),
                credential.getService().getName(),
                credential.getLoginIdentifier(),
                maskSecret(credential.getPasswordSecret()),
                maskSecret(credential.getRecoveryInfo()),
                maskSecret(credential.getTwoFactorSecret()),
                credential.getUsageNote(),
                credential.getInternalNote(),
                credential.getStatus(),
                credential.getAssignedOrder() == null ? null : credential.getAssignedOrder().getId(),
                credential.getAssignedOrder() == null ? null : credential.getAssignedOrder().getOrderCode(),
                credential.getReservedCheckout() == null ? null : credential.getReservedCheckout().getId(),
                credential.getReservedCheckout() == null ? null : credential.getReservedCheckout().getCheckoutCode(),
                credential.getReservedByUser() == null ? null : credential.getReservedByUser().getId(),
                credential.getDeliveredToUser() == null ? null : credential.getDeliveredToUser().getId(),
                credential.getDeliveredToUser() == null ? null : credential.getDeliveredToUser().getName(),
                credential.getDeliveredToUser() == null ? null : credential.getDeliveredToUser().getEmail(),
                credential.getDeliveredToUser() == null ? null : credential.getDeliveredToUser().getPhone(),
                credential.getReservedAt(),
                credential.getReservedUntil(),
                credential.getDeliveredAt(),
                credential.getExpiresAt(),
                credential.getWarrantyUntil(),
                credential.getCreatedAt());
    }

    private static String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return trimmed.substring(0, Math.min(2, trimmed.length())) + "****"
                + trimmed.substring(Math.max(trimmed.length() - 2, 2));
    }
}
