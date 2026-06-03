package com.example.KendyDigital.dto;

public record DepositQrResponse(
        String depositCode,
        String transferContent,
        String qrPayload,
        String qrImageUrl) {
}
