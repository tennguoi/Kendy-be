package com.example.KendyDigital.dto.wallet.response;


import com.example.KendyDigital.model.*;
import java.math.BigDecimal;
import java.time.Instant;

import com.example.KendyDigital.model.WalletTransaction;
import com.example.KendyDigital.model.WalletTransactionDirection;
import com.example.KendyDigital.model.WalletTransactionType;

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
