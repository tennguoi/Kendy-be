package com.example.KendyDigital.service.security;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorServiceImpl  implements TwoFactorService{
    private static final int SECRET_SIZE = 20;
    private static final int BACKUP_CODE_COUNT = 8;
    private static final int BACKUP_CODE_LENGTH = 10;
    private static final int TOTP_INTERVAL = 30;
    private static final int TOTP_DIGITS = 6;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String BACKUP_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final PasswordEncoder passwordEncoder;

    public TwoFactorServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String generateSecret() {
        byte[] bytes = new byte[SECRET_SIZE];
        RANDOM.nextBytes(bytes);
        StringBuilder secret = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            secret.append(BASE32_CHARS.charAt((bytes[i] & 0xFF) % BASE32_CHARS.length()));
            if ((i + 1) % 4 == 0 && i < bytes.length - 1) {
                secret.append(' ');
            }
        }
        return secret.toString().replace(" ", "");
    }

    public byte[] generateQRCode(String secret, String email, String issuer) {
        try {
            String otpAuth = "otpauth://totp/"
                    + urlEncode(issuer) + ":" + urlEncode(email)
                    + "?secret=" + secret
                    + "&issuer=" + urlEncode(issuer)
                    + "&algorithm=SHA1&digits=" + TOTP_DIGITS
                    + "&period=" + TOTP_INTERVAL;
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(otpAuth, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    public String qrCodeBase64(String secret, String email, String issuer) {
        return Base64.getEncoder().encodeToString(generateQRCode(secret, email, issuer));
    }

    public List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                code.append(BACKUP_CHARS.charAt(RANDOM.nextInt(BACKUP_CHARS.length())));
                if ((j + 1) % 5 == 0 && j < BACKUP_CODE_LENGTH - 1) {
                    code.append('-');
                }
            }
            codes.add(code.toString());
        }
        return codes;
    }

    public String hashStoredBackupCodes(List<String> plainCodes) {
        return plainCodes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.joining("\n"));
    }

    public boolean verify(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }
        try {
            long otp = Long.parseLong(code.trim());
            long timeWindow = System.currentTimeMillis() / 1000 / TOTP_INTERVAL;
            for (int i = -1; i <= 1; i++) {
                if (generateTOTP(secret, timeWindow + i) == otp) {
                    return true;
                }
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean verifyBackupCode(String storedCodes, String code) {
        if (storedCodes == null || code == null) {
            return false;
        }
        String trimmed = code.trim();
        for (String stored : splitBackupCodes(storedCodes)) {
            if (passwordEncoder.matches(trimmed, stored)) {
                return true;
            }
        }
        return false;
    }

    public String removeUsedBackupCode(String storedCodes, String usedCode) {
        List<String> remaining = new ArrayList<>();
        for (String stored : splitBackupCodes(storedCodes)) {
            if (!passwordEncoder.matches(usedCode.trim(), stored)) {
                remaining.add(stored);
            }
        }
        return String.join("\n", remaining);
    }

    private List<String> splitBackupCodes(String storedCodes) {
        List<String> codes = new ArrayList<>();
        String trimmed = storedCodes.trim();
        if (trimmed.isEmpty()) {
            return codes;
        }
        for (String part : trimmed.split("\\s*[,\n]\\s*")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                codes.add(p);
            }
        }
        return codes;
    }

    private long generateTOTP(String secret, long timeWindow) {
        try {
            byte[] key = Base32.decode(secret);
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (timeWindow & 0xFF);
                timeWindow >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0xF;
            long truncated = 0;
            for (int i = offset; i < offset + 4; i++) {
                truncated = (truncated << 8) | (hash[i] & 0xFF);
            }
            truncated &= 0x7FFFFFFF;
            return truncated % (long) Math.pow(10, TOTP_DIGITS);
        } catch (Exception e) {
            return -1;
        }
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    private static class Base32 {
        private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        private static final int[] INDEX = new int[128];

        static {
            for (int i = 0; i < INDEX.length; i++) {
                INDEX[i] = -1;
            }
            for (int i = 0; i < ALPHABET.length(); i++) {
                INDEX[ALPHABET.charAt(i)] = i;
            }
        }

        static byte[] decode(String encoded) {
            String cleaned = encoded.replace(" ", "").replace("-", "").toUpperCase();
            int byteLen = cleaned.length() * 5 / 8;
            byte[] result = new byte[byteLen];
            int buffer = 0;
            int bitsLeft = 0;
            int index = 0;
            for (int i = 0; i < cleaned.length(); i++) {
                int value = INDEX[cleaned.charAt(i)];
                if (value == -1) {
                    continue;
                }
                buffer = (buffer << 5) | value;
                bitsLeft += 5;
                if (bitsLeft >= 8) {
                    result[index++] = (byte) (buffer >> (bitsLeft - 8));
                    bitsLeft -= 8;
                }
            }
            return result;
        }
    }
}
