package com.example.KendyDigital.dto;


import com.example.KendyDigital.model.*;
import java.math.BigDecimal;
import java.time.Instant;

import com.example.KendyDigital.model.DepositRequest;
import com.example.KendyDigital.model.DepositStatus;

public record DepositResponse(
        Long id,
        String depositCode,
        Long userId,
        BigDecimal amount,
        String bankName,
        String bankAccount,
        String bankOwner,
        String transferContent,
        DepositStatus status,
        Instant expiredAt,
        Instant completedAt) {
    public static DepositResponse from(DepositRequest deposit) {
        return new DepositResponse(
                deposit.getId(),
                deposit.getDepositCode(),
                deposit.getUser().getId(),
                deposit.getAmount(),
                deposit.getBankName(),
                deposit.getBankAccount(),
                deposit.getBankOwner(),
                deposit.getTransferContent(),
                deposit.getStatus(),
                deposit.getExpiredAt(),
                deposit.getCompletedAt());
    }
}
