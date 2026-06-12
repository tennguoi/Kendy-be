package com.example.KendyDigital.dto.wallet.response;


import com.example.KendyDigital.model.*;
import java.math.BigDecimal;
import java.util.List;

public record WalletSummaryResponse(
        Long userId,
        BigDecimal balance,
        List<WalletTransactionResponse> recentTransactions) {
}
