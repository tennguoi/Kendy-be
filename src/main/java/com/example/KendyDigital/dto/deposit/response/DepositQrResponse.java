package com.example.KendyDigital.dto.deposit.response;

public record DepositQrResponse(
        String depositCode,
        String transferContent,
        String qrPayload,
        String qrImageUrl) {
}
