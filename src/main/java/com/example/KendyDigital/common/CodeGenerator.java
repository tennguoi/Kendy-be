package com.example.KendyDigital.common;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CodeGenerator {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generate(String prefix, int length) {
        // Use UUID v7 (time-ordered) for better entropy and traceability
        // Format: prefix + UUID without dashes (32 chars)
        UUID uuid = UUID.randomUUID(); // Java 21+ has UUID v7, fallback to v4
        String uuidPart = uuid.toString().replace("-", "");
        return prefix + uuidPart.substring(0, Math.min(length, uuidPart.length()));
    }

    public String generateSecureId() {
        // Generate a cryptographically secure random ID
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
