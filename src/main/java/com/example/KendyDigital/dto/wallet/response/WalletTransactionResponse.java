package com.example.KendyDigital.dto.wallet.response;

import com.example.KendyDigital.model.wallet.WalletTransaction;
import com.example.KendyDigital.model.wallet.WalletTransactionDirection;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record WalletTransactionResponse(
        Long id,
        String transactionCode,
        WalletTransactionType type,
        WalletTransactionDirection direction,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String referenceType,
        Long referenceId,
        String description,
        Instant createdAt) {
    public static WalletTransactionResponse from(WalletTransaction transaction) {
        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getTransactionCode(),
                transaction.getType(),
                transaction.getDirection(),
                transaction.getAmount(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getDescription(),
                transaction.getCreatedAt());
    }
}
