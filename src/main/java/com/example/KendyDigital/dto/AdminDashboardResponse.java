package com.example.KendyDigital.dto;

import java.math.BigDecimal;

public record AdminDashboardResponse(
        long totalUsers,
        long activeUsers,
        long lockedUsers,
        long pendingVerifyUsers,
        BigDecimal totalWalletBalance,
        long totalOrders,
        long processingOrders,
        long completedOrders,
        long cancelledOrders,
        long failedOrders,
        long refundedOrders,
        long pendingDeposits,
        long completedDeposits,
        long manualReviewDeposits,
        BigDecimal completedDepositAmount,
        long pendingAdminTickets,
        long pendingUserTickets,
        long resolvedTickets,
        long closedTickets) {
}
