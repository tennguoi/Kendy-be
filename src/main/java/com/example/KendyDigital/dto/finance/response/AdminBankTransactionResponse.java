package com.example.KendyDigital.dto.finance.response;

import com.example.KendyDigital.model.bank.BankTransaction;
import com.example.KendyDigital.model.bank.BankTransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminBankTransactionResponse(
        UUID id,
        Long sepayId,
        String gateway,
        String bankName,
        String accountNumber,
        String subAccount,
        Instant transactionDate,
        String transferType,
        BigDecimal transferAmount,
        String code,
        String content,
        String referenceCode,
        BankTransactionStatus status,
        String reviewReason,
        UUID matchedUserId,
        UUID matchedDepositRequestId,
        UUID walletTransactionId,
        Instant receivedAt,
        Instant creditedAt) {
    public static AdminBankTransactionResponse from(BankTransaction transaction) {
        return new AdminBankTransactionResponse(
                transaction.getPublicId(),
                transaction.getSepayId(),
                transaction.getGateway(),
                transaction.getBankName(),
                transaction.getAccountNumber(),
                transaction.getSubAccount(),
                transaction.getTransactionDate(),
                transaction.getTransferType(),
                transaction.getTransferAmount(),
                transaction.getCode(),
                transaction.getContent(),
                transaction.getReferenceCode(),
                transaction.getStatus(),
                transaction.getReviewReason(),
                transaction.getMatchedUser() == null ? null : transaction.getMatchedUser().getPublicId(),
                transaction.getMatchedDepositRequest() == null ? null : transaction.getMatchedDepositRequest().getPublicId(),
                transaction.getWalletTransaction() == null ? null : transaction.getWalletTransaction().getPublicId(),
                transaction.getReceivedAt(),
                transaction.getCreditedAt());
    }
}
