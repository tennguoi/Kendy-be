package com.example.KendyDigital.dto.user.response;

import com.example.KendyDigital.dto.auth.response.AuthUserResponse;
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
