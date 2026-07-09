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

@Converter
public class EncryptedCredentialAttributeConverter implements AttributeConverter<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(EncryptedCredentialAttributeConverter.class);
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final byte[] KEY = resolveKey();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank() || attribute.startsWith(PREFIX)) {
            return attribute;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot encrypt credential payload", exception);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || !dbData.startsWith(PREFIX)) {
            return dbData;
        }
        try {
            return decrypt(dbData, KEY);
        } catch (Exception primaryException) {
            String[] fallbackKeys = isProduction()
                    ? new String[0]
                    : new String[] {
                            "local-dev-only-kendy-credential-key-change-in-production",
                            "local-dev-fallback-kendy-credential-key",
                            "test-only-kendy-credential-key"
                    };
            for (String fallbackKeyStr : fallbackKeys) {
                try {
                    LOG.warn("CREDENTIAL_DECRYPT_FALLBACK: decrypting with dev/test key '{}...'",
                            fallbackKeyStr.substring(0, Math.min(fallbackKeyStr.length(), 20)));
                    return decrypt(dbData, sha256(fallbackKeyStr));
                } catch (Exception ignored) {
                }
            }
            LOG.error("CREDENTIAL_DECRYPT_FAILURE: cannot decrypt credential data with any available key. "
                    + "Check CREDENTIAL_ENCRYPTION_KEY environment variable.");
            return "[Encrypted - Key Mismatch: " + dbData.substring(0, Math.min(dbData.length(), 20)) + "...]";
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

    private static byte[] resolveKey() {
        // 1. Ưu tiên biến môi trường hệ thống
        String configured = firstNonBlank(
                System.getenv("CREDENTIAL_ENCRYPTION_KEY"),
                System.getenv("APP_CREDENTIAL_ENCRYPTION_KEY"),
                System.getProperty("credential.encryption.key"));

        // 2. Nếu trống, tìm trong .vscode/launch.json
        if (configured == null) {
            configured = readKeyFromLaunchJson();
        }

        // 3. Nếu vẫn trống và đang chạy trên Windows, truy vấn Registry hoặc chạy sinh khóa tự động
        if (configured == null && System.getProperty("os.name").toLowerCase().contains("win")) {
            configured = getOrGenerateWindowsUserKey();
        }

        // 4. Fallback mặc định cho môi trường Test, bắt buộc phải có key nếu chạy thật
        if (configured == null) {
            if (isTestRuntime()) {
                configured = "test-only-kendy-credential-key";
            } else {
                throw new IllegalStateException(
                        "CREDENTIAL_ENCRYPTION_KEY or APP_CREDENTIAL_ENCRYPTION_KEY is required");
            }
        }

        byte[] decoded = tryDecodeBase64(configured);
        if (decoded != null && (decoded.length == 16 || decoded.length == 24 || decoded.length == 32)) {
            return decoded;
        }
        return sha256(configured);
    }

    private static String readKeyFromLaunchJson() {
        String[] paths = {
            ".vscode/launch.json",
            "../.vscode/launch.json",
            "be/.vscode/launch.json",
            "../be/.vscode/launch.json",
            "/app/.vscode/launch.json",
            "/app/be/.vscode/launch.json",
            "/app/../.vscode/launch.json"
        };
        for (String path : paths) {
            java.io.File file = new java.io.File(path);
            if (file.exists() && file.isFile()) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                            "\"CREDENTIAL_ENCRYPTION_KEY\"\\s*:\\s*\"([^\"]+)\"");
                    java.util.regex.Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        return matcher.group(1).trim();
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static String getOrGenerateWindowsUserKey() {
        try {
            // Thử đọc từ User environment variables của Windows
            String key = executePowerShell("[Environment]::GetEnvironmentVariable('CREDENTIAL_ENCRYPTION_KEY', 'User')");
            if (key != null && !key.isBlank()) {
                return key.trim();
            }

            // Nếu không có, tự động chạy script generate-key.ps1
            String[] scriptPaths = {
                ".vscode/generate-key.ps1",
                "../.vscode/generate-key.ps1",
                "be/.vscode/generate-key.ps1",
                "../be/.vscode/generate-key.ps1"
            };
            for (String path : scriptPaths) {
                java.io.File script = new java.io.File(path);
                if (script.exists()) {
                    executeCommand("powershell.exe", "-ExecutionPolicy", "Bypass", "-File", path);
                    // Đọc lại sau khi chạy script
                    key = executePowerShell("[Environment]::GetEnvironmentVariable('CREDENTIAL_ENCRYPTION_KEY', 'User')");
                    if (key != null && !key.isBlank()) {
                        return key.trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String executePowerShell(String command) {
        return executeCommand("powershell.exe", "-Command", command);
    }

    private static String executeCommand(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            return null;
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

    private static boolean isTestRuntime() {
        String activeProfiles = firstNonBlank(
                System.getProperty("spring.profiles.active"),
                System.getenv("SPRING_PROFILES_ACTIVE"));
        if (activeProfiles != null && Arrays.stream(activeProfiles.split(","))
                .map(String::trim)
                .anyMatch(profile -> profile.equalsIgnoreCase("test"))) {
            return true;
        }
        return System.getProperty("surefire.test.class.path") != null;
    }

    private static boolean isProduction() {
        String activeProfiles = firstNonBlank(
                System.getProperty("spring.profiles.active"),
                System.getenv("SPRING_PROFILES_ACTIVE"));
        if (activeProfiles != null) {
            return Arrays.stream(activeProfiles.split(","))
                    .map(String::trim)
                    .anyMatch(profile -> profile.equalsIgnoreCase("prod")
                            || profile.equalsIgnoreCase("production"));
        }
        return false;
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
            throw new IllegalStateException("Cannot derive credential encryption key", exception);
        }
    }
}
