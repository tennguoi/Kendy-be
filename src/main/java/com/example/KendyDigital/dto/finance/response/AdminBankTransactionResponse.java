package com.example.KendyDigital.dto.finance.response;

import com.example.KendyDigital.model.bank.BankTransaction;
import com.example.KendyDigital.model.bank.BankTransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record AdminBankTransactionResponse(
        Long id,
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
        Long matchedUserId,
        Long matchedDepositRequestId,
        Long walletTransactionId,
        Instant receivedAt,
        Instant creditedAt) {
    public static AdminBankTransactionResponse from(BankTransaction transaction) {
        return new AdminBankTransactionResponse(
                transaction.getId(),
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
                transaction.getMatchedUser() == null ? null : transaction.getMatchedUser().getId(),
                transaction.getMatchedDepositRequest() == null ? null : transaction.getMatchedDepositRequest().getId(),
                transaction.getWalletTransaction() == null ? null : transaction.getWalletTransaction().getId(),
                transaction.getReceivedAt(),
                transaction.getCreditedAt());
    }
}
