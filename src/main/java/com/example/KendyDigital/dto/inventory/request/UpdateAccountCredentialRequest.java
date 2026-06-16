package com.example.KendyDigital.dto.inventory.request;

import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import java.time.Instant;

public record UpdateAccountCredentialRequest(
        String loginIdentifier,
        String passwordSecret,
        String recoveryInfo,
        String twoFactorSecret,
        String usageNote,
        String internalNote,
        AccountCredentialStatus status,
        Instant expiresAt,
        Instant warrantyUntil) {
}
