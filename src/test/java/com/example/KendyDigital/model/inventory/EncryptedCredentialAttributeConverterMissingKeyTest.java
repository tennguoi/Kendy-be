package com.example.KendyDigital.model.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for missing encryption key scenario.
 *
 * This test verifies that the converter throws an appropriate exception
 * when no encryption key is configured and no development fallback applies.
 */
class EncryptedCredentialAttributeConverterMissingKeyTest {

    @BeforeEach
    void clearKeyAndProfiles() {
        // Clear any existing key configuration
        System.clearProperty("credential.encryption.key");
        System.clearProperty("CREDENTIAL_ENCRYPTION_KEY");
        System.clearProperty("APP_CREDENTIAL_ENCRYPTION_KEY");
        // Clear active profiles to avoid triggering development fallback
        System.clearProperty("spring.profiles.active");
        System.clearProperty("SPRING_PROFILES_ACTIVE");
        System.clearProperty("app.security.credential-encryption-key");
    }

    @AfterEach
    void restoreTestKey() {
        // Restore a test key so other tests aren't affected
        System.setProperty("credential.encryption.key", "test-restoration-key");
    }

    @Test
    void missingKeyThrowsExceptionWhenNoProfilesActive() {
        // Given/When/Then
        assertThrows(IllegalStateException.class, () -> {
            new EncryptedCredentialAttributeConverter();
        });
    }

    @Test
    void developmentFallbackUsedWhenTestProfileActive() {
        // Given
        System.setProperty("spring.profiles.active", "test");
        System.clearProperty("credential.encryption.key");
        System.clearProperty("CREDENTIAL_ENCRYPTION_KEY");
        System.clearProperty("APP_CREDENTIAL_ENCRYPTION_KEY");

        // When
        EncryptedCredentialAttributeConverter converter = new EncryptedCredentialAttributeConverter();

        // Then - should not throw exception, should use development key
        assertNotNull(converter);

        // Test that it actually works
        String testValue = "test-value";
        String encrypted = converter.convertToDatabaseColumn(testValue);
        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertEquals(testValue, decrypted);

        // Clean up
        System.clearProperty("spring.profiles.active");
    }
}