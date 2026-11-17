package com.example.KendyDigital.dto.warranty.response;

import com.example.KendyDigital.model.warranty.WarrantyRequest;
import com.example.KendyDigital.model.warranty.WarrantyRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record WarrantyRequestResponse(
        UUID id,
        String orderCode,
        UUID userId,
        UUID serviceId,
        String serviceName,
        UUID originalCredentialId,
        UUID replacementCredentialId,
        UUID refundWalletTransactionId,
        String reason,
        String evidenceText,
        WarrantyRequestStatus status,
        String adminNote,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt) {
    public static WarrantyRequestResponse from(WarrantyRequest request) {
        return new WarrantyRequestResponse(
                request.getPublicId(),
                request.getOrder().getOrderCode(),
                request.getUser().getPublicId(),
                request.getOrder().getService().getPublicId(),
                request.getOrder().getService().getName(),
                request.getOriginalCredential() == null ? null : request.getOriginalCredential().getPublicId(),
                request.getReplacementCredential() == null ? null : request.getReplacementCredential().getPublicId(),
                request.getRefundWalletTransaction() == null ? null : request.getRefundWalletTransaction().getPublicId(),
                request.getReason(),
                request.getEvidenceText(),
                request.getStatus(),
                request.getAdminNote(),
                request.getResolvedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
