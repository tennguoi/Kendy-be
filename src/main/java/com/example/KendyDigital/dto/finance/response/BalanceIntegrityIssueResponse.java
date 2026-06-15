package com.example.KendyDigital.dto.finance.response;

import java.math.BigDecimal;

public record BalanceIntegrityIssueResponse(
        Long userId,
        String email,
        BigDecimal storedBalance,
        BigDecimal ledgerBalance,
        BigDecimal difference) {
}
