package com.example.KendyDigital.service.wallet;

import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.wallet.WalletTransaction;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import com.example.KendyDigital.repository.*;
import java.math.BigDecimal;

public interface WalletLedgerService {
    WalletTransaction credit(UserAccount user, BigDecimal amount, WalletTransactionType type, String referenceType, Long referenceId, String description, Long createdBy);
    WalletTransaction debit(UserAccount user, BigDecimal amount, WalletTransactionType type, String referenceType, Long referenceId, String description, Long createdBy);
}
