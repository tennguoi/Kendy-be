package com.example.KendyDigital.model.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test for the simplified EncryptedCredentialAttributeConverter.
 *
 * This test verifies that the simplified encryption/decryption works correctly
 * with various inputs including null, empty, and actual credential values.
 */
@SpringBootTest
class EncryptedCredentialAttributeConverterTest {

    private EncryptedCredentialAttributeConverter converter;

    @BeforeEach
    void setUp() {
        // Set the encryption key for testing
        System.setProperty("credential.encryption.key", "test-encryption-key-for-testing-only");
        converter = new EncryptedCredentialAttributeConverter();
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties
        System.clearProperty("credential.encryption.key");
    }

    @Test
    void convertNullAttributeReturnsNull() {
        // When
        String result = converter.convertToDatabaseColumn(null);

        // Then
        assertEquals(null, result);
    }

    @Test
    void convertEmptyAttributeReturnsEmpty() {
        // When
        String result = converter.convertToDatabaseColumn("");

        // Then
        assertEquals("", result);
    }

    @Test
    void convertToDatabaseColumnEncryptsValue() {
        // Given
        String plainText = "my-secret-password";

        // When
        String encrypted = converter.convertToDatabaseColumn(plainText);

        // Then
        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("enc:v1:"));
        assertTrue(encrypted.length() > "enc:v1:".length());
        // Should not be the same as the plain text
        assertEquals(false, encrypted.equals(plainText));
    }

    @Test
    void convertToEntityAttributeDecryptsValue() {
        // Given
        String plainText = "my-secret-password";
        String encrypted = converter.convertToDatabaseColumn(plainText);

        // When
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertEquals(plainText, decrypted);
    }

    @Test
    void convertToEntityAttributeReturnsNullForNullInput() {
        // When
        String result = converter.convertToEntityAttribute(null);

        // Then
        assertEquals(null, result);
    }

    @Test
    void convertToEntityAttributeReturnsEmptyForEmptyInput() {
        // When
        String result = converter.convertToEntityAttribute("");

        // Then
        assertEquals("", result);
    }

    @Test
    void convertToEntityAttributeReturnsUnprefixedValuesAsIs() {
        // Given
        String unprefixed = "plain-text-value";

        // When
        String result = converter.convertToEntityAttribute(unprefixed);

        // Then
        assertEquals(unprefixed, result);
    }

    @Test
    void roundTripEncryptionDecryption() {
        // Given
        String original = "sensitive-credential-data-123!@#";

        // When
        String encrypted = converter.convertToDatabaseColumn(original);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        // Then
        assertEquals(original, decrypted);
    }

    @Test
    void differentInputsProduceDifferentEncryptions() {
        // Given
        String text1 = "first-password";
        String text2 = "second-password";

        // When
        String encrypted1 = converter.convertToDatabaseColumn(text1);
        String encrypted2 = converter.convertToDatabaseColumn(text2);

        // Then
        // Due to random IV, same inputs should produce different outputs
        assertEquals(false, encrypted1.equals(encrypted2));
    }

    @Test
    void developmentFallbackKeyIsUsedWhenNoOtherKeyAvailableAndTestProfileActive() {
        // Given
        System.clearProperty("credential.encryption.key");
        System.clearProperty("CREDENTIAL_ENCRYPTION_KEY");
        System.setProperty("spring.profiles.active", "test");

        // When
        EncryptedCredentialAttributeConverter devConverter = new EncryptedCredentialAttributeConverter();

        // Then
        assertNotNull(devConverter);

        // Test that it actually works
        String testValue = "test-value";
        String encrypted = devConverter.convertToDatabaseColumn(testValue);
        String decrypted = devConverter.convertToEntityAttribute(encrypted);
        assertEquals(testValue, decrypted);

        // Clean up
        System.clearProperty("spring.profiles.active");
    }
}