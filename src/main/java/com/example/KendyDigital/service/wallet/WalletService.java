package com.example.KendyDigital.service.wallet;

import com.example.KendyDigital.dto.wallet.response.WalletSummaryResponse;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import com.example.KendyDigital.model.wallet.WalletTransactionDirection;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import java.util.List;

public interface WalletService {
    WalletSummaryResponse getWallet(Long userId);
    List<WalletTransactionResponse> listTransactions(Long userId, WalletTransactionType type, WalletTransactionDirection direction);
    List<WalletTransactionResponse> listTransactions(Long userId, WalletTransactionType type, WalletTransactionDirection direction, int page, int size);
    List<WalletTransactionResponse> searchTransactions(Long userId, String query, WalletTransactionType type, WalletTransactionDirection direction, int page, int size);
    WalletTransactionResponse getTransaction(Long userId, Long id);
}
