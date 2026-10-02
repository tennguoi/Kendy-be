package com.example.KendyDigital.dto.inventory.request;

import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateAccountCredentialRequest(
        @Size(max = 255) String loginIdentifier,
        @Size(max = 2000) String passwordSecret,
        @Size(max = 2000) String recoveryInfo,
        @Size(max = 500) String twoFactorSecret,
        @Size(max = 2000) String usageNote,
        @Size(max = 2000) String internalNote,
        AccountCredentialStatus status,
        Instant expiresAt,
        Instant warrantyUntil,
        Long version) {

    public UpdateAccountCredentialRequest(
            String loginIdentifier, String passwordSecret, String recoveryInfo,
            String twoFactorSecret, String usageNote, String internalNote,
            AccountCredentialStatus status, Instant expiresAt, Instant warrantyUntil) {
        this(loginIdentifier, passwordSecret, recoveryInfo, twoFactorSecret,
                usageNote, internalNote, status, expiresAt, warrantyUntil, null);
    }
}
