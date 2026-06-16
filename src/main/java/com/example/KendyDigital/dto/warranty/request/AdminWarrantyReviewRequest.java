package com.example.KendyDigital.dto.warranty.request;

import com.example.KendyDigital.model.warranty.WarrantyRequestStatus;
import jakarta.validation.constraints.NotNull;

public record AdminWarrantyReviewRequest(
        @NotNull WarrantyRequestStatus status,
        Long replacementCredentialId,
        String adminNote) {
}
