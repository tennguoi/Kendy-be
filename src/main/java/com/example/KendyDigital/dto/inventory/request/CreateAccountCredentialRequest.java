package com.example.KendyDigital.dto.inventory.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateAccountCredentialRequest(
        @NotBlank @Size(max = 255) String loginIdentifier,
        @NotBlank @Size(max = 2000) String passwordSecret,
        @Size(max = 2000) String recoveryInfo,
        @Size(max = 500) String twoFactorSecret,
        @Size(max = 2000) String usageNote,
        @Size(max = 2000) String internalNote,
        Instant expiresAt,
        Instant warrantyUntil) {
}
