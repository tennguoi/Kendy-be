package com.example.KendyDigital.dto.wallet.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record WalletSummaryResponse(
        UUID userId,
        BigDecimal balance,
        List<WalletTransactionResponse> recentTransactions) {
}
