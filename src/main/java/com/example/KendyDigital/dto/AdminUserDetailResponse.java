package com.example.KendyDigital.dto;

import java.math.BigDecimal;

public record AdminUserDetailResponse(
        AdminUserResponse user,
        long orderCount,
        long depositCount,
        long walletTransactionCount,
        long ticketCount,
        BigDecimal completedDepositAmount,
        BigDecimal purchaseAmount,
        BigDecimal refundAmount) {
}
