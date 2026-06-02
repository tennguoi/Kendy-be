package com.example.KendyDigital.service;




import com.example.KendyDigital.dto.*;
import com.example.KendyDigital.repository.*;
import com.example.KendyDigital.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.common.CodeGenerator;

@Service
public class WalletLedgerService {
    private final WalletTransactionRepository walletTransactionRepository;
    private final CodeGenerator codeGenerator;

    public WalletLedgerService(WalletTransactionRepository walletTransactionRepository, CodeGenerator codeGenerator) {
        this.walletTransactionRepository = walletTransactionRepository;
        this.codeGenerator = codeGenerator;
    }

    public WalletTransaction credit(UserAccount user, BigDecimal amount, WalletTransactionType type,
            String referenceType, Long referenceId, String description, Long createdBy) {
        BigDecimal normalizedAmount = normalizePositiveAmount(amount);
        BigDecimal balanceBefore = normalizeMoney(user.getBalance());
        BigDecimal balanceAfter = balanceBefore.add(normalizedAmount);
        user.setBalance(balanceAfter);

        WalletTransaction transaction = new WalletTransaction(
                nextTransactionCode(),
                user,
                type,
                WalletTransactionDirection.CREDIT,
                normalizedAmount,
                balanceBefore,
                balanceAfter,
                referenceType,
                referenceId,
                description,
                createdBy);
        return walletTransactionRepository.save(transaction);
    }

    public WalletTransaction debit(UserAccount user, BigDecimal amount, WalletTransactionType type,
            String referenceType, Long referenceId, String description, Long createdBy) {
        BigDecimal normalizedAmount = normalizePositiveAmount(amount);
        BigDecimal balanceBefore = normalizeMoney(user.getBalance());
        BigDecimal balanceAfter = balanceBefore.subtract(normalizedAmount);
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }
        user.setBalance(balanceAfter);

        WalletTransaction transaction = new WalletTransaction(
                nextTransactionCode(),
                user,
                type,
                WalletTransactionDirection.DEBIT,
                normalizedAmount,
                balanceBefore,
                balanceAfter,
                referenceType,
                referenceId,
                description,
                createdBy);
        return walletTransactionRepository.save(transaction);
    }

    private BigDecimal normalizePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String nextTransactionCode() {
        String code;
        do {
            code = codeGenerator.generate("WT", 12);
        } while (walletTransactionRepository.existsByTransactionCode(code));
        return code;
    }
}
