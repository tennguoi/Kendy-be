package com.example.KendyDigital.dto.warranty.request;


import jakarta.validation.constraints.Size;
import com.example.KendyDigital.model.warranty.WarrantyRequestStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdminWarrantyReviewRequest(
        @NotNull WarrantyRequestStatus status,
        UUID replacementCredentialId,
        @Size(max = 1000) String adminNote) {
}
