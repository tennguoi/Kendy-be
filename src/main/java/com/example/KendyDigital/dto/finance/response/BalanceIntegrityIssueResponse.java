package com.example.KendyDigital.dto.finance.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceIntegrityIssueResponse(
        UUID userId,
        String email,
        BigDecimal storedBalance,
        BigDecimal ledgerBalance,
        BigDecimal difference) {
}
