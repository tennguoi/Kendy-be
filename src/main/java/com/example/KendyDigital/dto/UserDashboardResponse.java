package com.example.KendyDigital.dto;

import java.math.BigDecimal;

public record UserDashboardResponse(
        AuthUserResponse user,
        BigDecimal balance,
        long orderCount,
        long processingOrders,
        long completedOrders,
        long cancelledOrders,
        long depositCount,
        long pendingDeposits,
        long completedDeposits,
        BigDecimal completedDepositAmount,
        long ticketCount,
        long pendingAdminTickets,
        long pendingUserTickets,
        long walletTransactionCount,
        BigDecimal purchaseAmount,
        BigDecimal refundAmount) {
}
