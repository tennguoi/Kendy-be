package com.example.KendyDigital.service.security.qrcode;

public interface QrCodeService {
    byte[] generateQrCodePng(String text, int width, int height);
    String generateQrCodeBase64(String text, int width, int height);
    byte[] generateTotpQrCodePng(String secret, String email, String issuer);
    String generateTotpQrCodeBase64(String secret, String email, String issuer);
}
