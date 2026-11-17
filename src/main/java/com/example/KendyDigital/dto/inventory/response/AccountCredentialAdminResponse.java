package com.example.KendyDigital.dto.inventory.response;

import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import java.time.Instant;
import java.util.UUID;

public record AccountCredentialAdminResponse(
        UUID id,
        UUID serviceId,
        String serviceName,
        String loginIdentifier,
        String passwordSecret,
        String recoveryInfo,
        String twoFactorSecret,
        String usageNote,
        String internalNote,
        AccountCredentialStatus status,
        UUID assignedOrderId,
        String assignedOrderCode,
        UUID reservedCheckoutId,
        String reservedCheckoutCode,
        UUID reservedByUserId,
        UUID deliveredToUserId,
        String deliveredToName,
        String deliveredToEmail,
        String deliveredToPhone,
        Instant reservedAt,
        Instant reservedUntil,
        Instant deliveredAt,
        Instant expiresAt,
        Instant warrantyUntil,
        Instant createdAt,
        Long version) {

    public AccountCredentialAdminResponse(
            UUID id, UUID serviceId, String serviceName, String loginIdentifier,
            String passwordSecret, String recoveryInfo, String twoFactorSecret,
            String usageNote, String internalNote, AccountCredentialStatus status,
            UUID assignedOrderId, String assignedOrderCode, UUID reservedCheckoutId,
            String reservedCheckoutCode, UUID reservedByUserId, UUID deliveredToUserId,
            String deliveredToName, String deliveredToEmail, String deliveredToPhone,
            Instant reservedAt, Instant reservedUntil, Instant deliveredAt,
            Instant expiresAt, Instant warrantyUntil, Instant createdAt) {
        this(id, serviceId, serviceName, loginIdentifier, passwordSecret, recoveryInfo,
                twoFactorSecret, usageNote, internalNote, status, assignedOrderId,
                assignedOrderCode, reservedCheckoutId, reservedCheckoutCode, reservedByUserId,
                deliveredToUserId, deliveredToName, deliveredToEmail, deliveredToPhone,
                reservedAt, reservedUntil, deliveredAt, expiresAt, warrantyUntil,
                createdAt, 0L);
    }

    public static AccountCredentialAdminResponse from(AccountCredential credential) {
        return new AccountCredentialAdminResponse(
                credential.getPublicId(),
                credential.getService().getPublicId(),
                credential.getService().getName(),
                credential.getLoginIdentifier(),
                maskSecret(credential.getPasswordSecret()),
                maskSecret(credential.getRecoveryInfo()),
                maskSecret(credential.getTwoFactorSecret()),
                credential.getUsageNote(),
                credential.getInternalNote(),
                credential.getStatus(),
                credential.getAssignedOrder() == null ? null : credential.getAssignedOrder().getPublicId(),
                credential.getAssignedOrder() == null ? null : credential.getAssignedOrder().getOrderCode(),
                credential.getReservedCheckout() == null ? null : credential.getReservedCheckout().getPublicId(),
                credential.getReservedCheckout() == null ? null : credential.getReservedCheckout().getCheckoutCode(),
                credential.getReservedByUser() == null ? null : credential.getReservedByUser().getPublicId(),
                credential.getDeliveredToUser() == null ? null : credential.getDeliveredToUser().getPublicId(),
                credential.getDeliveredToUser() == null ? null : credential.getDeliveredToUser().getName(),
                credential.getDeliveredToUser() == null ? null : credential.getDeliveredToUser().getEmail(),
                credential.getDeliveredToUser() == null ? null : credential.getDeliveredToUser().getPhone(),
                credential.getReservedAt(),
                credential.getReservedUntil(),
                credential.getDeliveredAt(),
                credential.getExpiresAt(),
                credential.getWarrantyUntil(),
                credential.getCreatedAt(),
                credential.getVersion());
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
