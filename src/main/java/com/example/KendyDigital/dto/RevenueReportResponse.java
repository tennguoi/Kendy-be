package com.example.KendyDigital.dto;

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
