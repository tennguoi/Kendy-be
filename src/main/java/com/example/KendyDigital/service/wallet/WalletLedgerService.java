package com.example.KendyDigital.service.wallet;

import com.example.KendyDigital.model.wallet.WalletTransaction;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import java.math.BigDecimal;

public interface WalletLedgerService {
    WalletTransaction credit(Long userId, BigDecimal amount, WalletTransactionType type, String referenceType, Long referenceId, String description, Long createdBy);
    WalletTransaction debit(Long userId, BigDecimal amount, WalletTransactionType type, String referenceType, Long referenceId, String description, Long createdBy);
}
