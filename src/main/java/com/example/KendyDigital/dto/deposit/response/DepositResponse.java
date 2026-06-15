package com.example.KendyDigital.dto.deposit.response;

import com.example.KendyDigital.model.deposit.DepositRequest;
import com.example.KendyDigital.model.deposit.DepositStatus;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public record DepositResponse(
        Long id,
        String depositCode,
        Long userId,
        BigDecimal amount,
        String bankName,
        String bankAccount,
        String bankOwner,
        String transferContent,
        String qrPayload,
        String qrImageUrl,
        DepositStatus status,
        Instant expiredAt,
        Instant completedAt) {
    public static DepositResponse from(DepositRequest deposit) {
        String qrPayload = vietQrPayload(deposit);
        return new DepositResponse(
                deposit.getId(),
                deposit.getDepositCode(),
                deposit.getUser().getId(),
                deposit.getAmount(),
                deposit.getBankName(),
                deposit.getBankAccount(),
                deposit.getBankOwner(),
                deposit.getTransferContent(),
                qrPayload,
                vietQrImageUrl(deposit),
                deposit.getStatus(),
                deposit.getExpiredAt(),
                deposit.getCompletedAt());
    }

    private static String vietQrPayload(DepositRequest deposit) {
        return "bank=" + deposit.getBankName()
                + ";account=" + deposit.getBankAccount()
                + ";amount=" + deposit.getAmount().toPlainString()
                + ";content=" + deposit.getTransferContent()
                + ";name=" + deposit.getBankOwner();
    }

    private static String vietQrImageUrl(DepositRequest deposit) {
        return "https://img.vietqr.io/image/"
                + encode(deposit.getBankName()) + "-"
                + encode(deposit.getBankAccount()) + "-compact2.png"
                + "?amount=" + encode(deposit.getAmount().toPlainString())
                + "&addInfo=" + encode(deposit.getTransferContent())
                + "&accountName=" + encode(deposit.getBankOwner());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
