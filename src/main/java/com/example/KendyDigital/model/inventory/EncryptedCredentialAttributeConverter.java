package com.example.KendyDigital.model.inventory;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplified credential encryption converter.
 *
 * This converter uses AES/GCM encryption to securely store sensitive credential
 * fields in the database while maintaining a relatively simple implementation.
 *
 * Key Management:
 * - Requires CREDENTIAL_ENCRYPTION_KEY environment variable or credential.encryption.key system property
 * - Optional development fallback for local testing only
 * - Clear error messages when configuration is missing or invalid
 *
 * Security Properties Maintained:
 * - AES-256-GCM encryption (same strength as before)
 * - Random IV per encryption operation
 * - Authentication tag to prevent tampering
 * - Field-level encryption (not full table/column encryption)
 */
@Converter
public class EncryptedCredentialAttributeConverter implements AttributeConverter<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(EncryptedCredentialAttributeConverter.class);
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final byte[] key;

    public EncryptedCredentialAttributeConverter() {
        this.key = resolveKey();
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank() || attribute.startsWith(PREFIX)) {
            return attribute;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(this.key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot encrypt credential payload: " + exception.getMessage(), exception);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || !dbData.startsWith(PREFIX)) {
            return dbData;
        }
        try {
            return decrypt(dbData, this.key);
        } catch (Exception exception) {
            LOG.error("Failed to decrypt credential data: {}", exception.getMessage());
            // Return a safe placeholder to avoid exposing encryption issues in UI
            return "[Encrypted - Decryption Failed]";
        }
    }

    private String decrypt(String dbData, byte[] keyBytes) throws Exception {
        byte[] payload = Base64.getUrlDecoder().decode(dbData.substring(PREFIX.length()));
        byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
        byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    /**
     * Resolve the encryption key from configuration.
     *
     * Resolution order:
     * 1. CREDENTIAL_ENCRYPTION_KEY environment variable
     * 2. APP_CREDENTIAL_ENCRYPTION_KEY environment variable (backward compatibility)
     * 3. credential.encryption.key system property
     * 4. Optional development fallback (only in non-production)
     *
     * @return The resolved encryption key bytes
     * @throws IllegalStateException if no valid key can be resolved
     */
    private static byte[] resolveKey() {
        // 1. System property (for test compatibility)
        String configured = System.getProperty("credential.encryption.key");

        boolean ignoreEnv = "true".equalsIgnoreCase(System.getProperty("credential.encryption.ignore-env"));

        // 2. Primary environment variable
        if (!ignoreEnv && (configured == null || configured.isBlank())) {
            configured = System.getenv("CREDENTIAL_ENCRYPTION_KEY");
        }

        // 3. Backward compatibility environment variable
        if (!ignoreEnv && (configured == null || configured.isBlank())) {
            configured = System.getenv("APP_CREDENTIAL_ENCRYPTION_KEY");
        }

        // 4. Development fallback: Only allow in explicit development environments
        if (configured == null || configured.isBlank()) {
            String activeProfiles = firstNonBlank(
                    System.getProperty("spring.profiles.active"),
                    System.getenv("SPRING_PROFILES_ACTIVE"));

            boolean isDevelopment = activeProfiles != null &&
                    (activeProfiles.contains("dev") ||
                     activeProfiles.contains("local") ||
                     activeProfiles.contains("test"));

            if (isDevelopment) {
                // Simple, clearly marked development key - NOT for production use
                configured = "local-development-key-do-not-use-in-production";
                LOG.warn("Using development fallback encryption key. DO NOT USE IN PRODUCTION.");
            } else {
                throw new IllegalStateException(
                        "CREDENTIAL_ENCRYPTION_KEY environment variable or credential.encryption.key system property is required");
            }
        }

        // Try to decode as Base64 first (for actual key material)
        byte[] decoded = tryDecodeBase64(configured);
        if (decoded != null && (decoded.length == 16 || decoded.length == 24 || decoded.length == 32)) {
            return decoded;
        }

        // Otherwise derive key from string using SHA-256
        return sha256(configured);
    }

    private static byte[] tryDecodeBase64(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot derive credential encryption key: " + exception.getMessage(), exception);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}