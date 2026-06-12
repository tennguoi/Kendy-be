package com.example.KendyDigital.dto.user.response;

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
