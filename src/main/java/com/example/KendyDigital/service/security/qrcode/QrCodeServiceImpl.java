package com.example.KendyDigital.service.security.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Override
    public byte[] generateQrCodePng(String text, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Override
    public String generateQrCodeBase64(String text, int width, int height) {
        return Base64.getEncoder().encodeToString(generateQrCodePng(text, width, height));
    }

    @Override
    public byte[] generateTotpQrCodePng(String secret, String email, String issuer) {
        String otpAuth = "otpauth://totp/"
                + urlEncode(issuer) + ":" + urlEncode(email)
                + "?secret=" + secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=6"
                + "&period=30";
        return generateQrCodePng(otpAuth, 300, 300);
    }

    @Override
    public String generateTotpQrCodeBase64(String secret, String email, String issuer) {
        return Base64.getEncoder().encodeToString(generateTotpQrCodePng(secret, email, issuer));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
