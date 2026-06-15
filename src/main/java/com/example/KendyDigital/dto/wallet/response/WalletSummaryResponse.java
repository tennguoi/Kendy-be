package com.example.KendyDigital.dto.wallet.response;

import java.math.BigDecimal;
import java.util.List;

public record WalletSummaryResponse(
        Long userId,
        BigDecimal balance,
        List<WalletTransactionResponse> recentTransactions) {
}
