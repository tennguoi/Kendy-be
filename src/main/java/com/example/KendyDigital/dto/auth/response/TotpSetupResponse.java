package com.example.KendyDigital.dto.auth.response;

import java.util.List;

public record TotpSetupResponse(
        String secret,
        String qrCodeBase64,
        List<String> backupCodes) {
}
