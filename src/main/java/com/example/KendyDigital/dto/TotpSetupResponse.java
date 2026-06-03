package com.example.KendyDigital.dto;

import java.util.List;

public record TotpSetupResponse(
        String secret,
        String qrCodeBase64,
        List<String> backupCodes) {
}
