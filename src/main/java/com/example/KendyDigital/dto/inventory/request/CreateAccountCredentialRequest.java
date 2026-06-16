package com.example.KendyDigital.dto.inventory.request;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record CreateAccountCredentialRequest(
        @NotBlank String loginIdentifier,
        @NotBlank String passwordSecret,
        String recoveryInfo,
        String twoFactorSecret,
        String usageNote,
        String internalNote,
        Instant expiresAt,
        Instant warrantyUntil) {
}
