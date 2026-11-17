package com.example.KendyDigital.service.wallet;

import com.example.KendyDigital.dto.user.request.AdminWalletAdjustmentRequest;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import com.example.KendyDigital.model.wallet.WalletTransactionDirection;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdminWalletManagerService {
    List<WalletTransactionResponse> listWalletTransactions(UUID userId);
    List<WalletTransactionResponse> listWalletTransactions(UUID userId, Instant fromDate, Instant toDate, int page, int size);
    List<WalletTransactionResponse> listWalletTransactions(UUID userId, Integer limit);
    List<WalletTransactionResponse> searchWalletTransactions(String query, UUID userId, WalletTransactionType type, WalletTransactionDirection direction, Instant fromDate, Instant toDate, int page, int size);
    List<WalletTransactionResponse> searchWalletTransactions(String query, UUID userId, WalletTransactionType type, WalletTransactionDirection direction, Integer limit);
    WalletTransactionResponse getWalletTransaction(UUID walletTransactionId);
    WalletTransactionResponse adjustWallet(Long adminUserId, UUID targetUserId, AdminWalletAdjustmentRequest request);
}
