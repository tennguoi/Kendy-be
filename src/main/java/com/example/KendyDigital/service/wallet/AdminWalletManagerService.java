package com.example.KendyDigital.service.wallet;

import com.example.KendyDigital.dto.user.request.AdminWalletAdjustmentRequest;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import com.example.KendyDigital.model.wallet.WalletTransactionDirection;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import java.time.Instant;
import java.util.List;

public interface AdminWalletManagerService {
    List<WalletTransactionResponse> listWalletTransactions(Long userId);
    List<WalletTransactionResponse> listWalletTransactions(Long userId, Instant fromDate, Instant toDate, int page, int size);
    List<WalletTransactionResponse> listWalletTransactions(Long userId, Integer limit);
    List<WalletTransactionResponse> searchWalletTransactions(String query, Long userId, WalletTransactionType type, WalletTransactionDirection direction, Instant fromDate, Instant toDate, int page, int size);
    List<WalletTransactionResponse> searchWalletTransactions(String query, Long userId, WalletTransactionType type, WalletTransactionDirection direction, Integer limit);
    WalletTransactionResponse getWalletTransaction(Long walletTransactionId);
    WalletTransactionResponse adjustWallet(Long adminUserId, Long targetUserId, AdminWalletAdjustmentRequest request);
}
