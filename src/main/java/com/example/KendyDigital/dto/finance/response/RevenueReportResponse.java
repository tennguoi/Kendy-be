package com.example.KendyDigital.dto.finance.response;

import java.math.BigDecimal;

public record RevenueReportResponse(
        BigDecimal depositVolume,
        BigDecimal grossRevenue,
        BigDecimal totalRefunds,
        BigDecimal netRevenue,
        BigDecimal walletLiability,
        BigDecimal totalCost,
        BigDecimal profit) {
}
