package com.example.KendyDigital.service.product_inventory.helper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CredentialHasher {

    public String computePayloadHash(Long serviceId, String loginIdentifier, String passwordSecret,
            String recoveryInfo, String twoFactorSecret) {
        String normalized = serviceId + "|"
                + nullToEmpty(loginIdentifier).toLowerCase(Locale.ROOT).trim() + "|"
                + nullToEmpty(passwordSecret).trim() + "|"
                + nullToEmpty(recoveryInfo).trim() + "|"
                + nullToEmpty(twoFactorSecret).trim();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash credential payload", exception);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
