package com.example.KendyDigital.dto.warranty.response;

import com.example.KendyDigital.model.warranty.WarrantyRequest;
import com.example.KendyDigital.model.warranty.WarrantyRequestStatus;
import java.time.Instant;

public record WarrantyRequestResponse(
        Long id,
        String orderCode,
        Long userId,
        Long serviceId,
        String serviceName,
        Long originalCredentialId,
        Long replacementCredentialId,
        Long refundWalletTransactionId,
        String reason,
        String evidenceText,
        WarrantyRequestStatus status,
        String adminNote,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt) {
    public static WarrantyRequestResponse from(WarrantyRequest request) {
        return new WarrantyRequestResponse(
                request.getId(),
                request.getOrder().getOrderCode(),
                request.getUser().getId(),
                request.getOrder().getService().getId(),
                request.getOrder().getService().getName(),
                request.getOriginalCredential() == null ? null : request.getOriginalCredential().getId(),
                request.getReplacementCredential() == null ? null : request.getReplacementCredential().getId(),
                request.getRefundWalletTransaction() == null ? null : request.getRefundWalletTransaction().getId(),
                request.getReason(),
                request.getEvidenceText(),
                request.getStatus(),
                request.getAdminNote(),
                request.getResolvedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
