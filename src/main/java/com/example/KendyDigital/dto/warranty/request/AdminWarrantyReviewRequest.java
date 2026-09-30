package com.example.KendyDigital.dto.warranty.request;


import jakarta.validation.constraints.Size;
import com.example.KendyDigital.model.warranty.WarrantyRequestStatus;
import jakarta.validation.constraints.NotNull;

public record AdminWarrantyReviewRequest(
        @NotNull WarrantyRequestStatus status,
        Long replacementCredentialId,
        @Size(max = 1000) String adminNote) {
}
