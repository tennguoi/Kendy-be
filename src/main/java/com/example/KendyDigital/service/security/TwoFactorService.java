package com.example.KendyDigital.service.security;

import java.util.List;

public interface TwoFactorService {
    String generateSecret();
    byte[] generateQRCode(String secret, String email, String issuer);
    String qrCodeBase64(String secret, String email, String issuer);
    List<String> generateBackupCodes();
    String hashStoredBackupCodes(List<String> plainCodes);
    boolean verify(String secret, String code);
    boolean verifyBackupCode(String storedCodes, String code);
    String removeUsedBackupCode(String storedCodes, String usedCode);
}
