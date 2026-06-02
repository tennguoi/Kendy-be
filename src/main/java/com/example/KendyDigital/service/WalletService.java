package com.example.KendyDigital.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.WalletSummaryResponse;
import com.example.KendyDigital.dto.WalletTransactionResponse;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.WalletTransaction;
import com.example.KendyDigital.model.WalletTransactionDirection;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;

@Service
public class WalletService {
    private final UserAccountRepository userAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(UserAccountRepository userAccountRepository,
            WalletTransactionRepository walletTransactionRepository) {
        this.userAccountRepository = userAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional(readOnly = true)
    public WalletSummaryResponse getWallet(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new WalletSummaryResponse(
                user.getId(),
                user.getBalance(),
                listTransactions(userId, null, null));
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listTransactions(Long userId, WalletTransactionType type,
            WalletTransactionDirection direction) {
        List<WalletTransaction> transactions;
        if (type != null && direction != null) {
            transactions = walletTransactionRepository
                    .findAllByUser_IdAndTypeAndDirectionOrderByCreatedAtDesc(userId, type, direction, PageRequest.of(0, 50));
        } else if (type != null) {
            transactions = walletTransactionRepository.findAllByUser_IdAndTypeOrderByCreatedAtDesc(userId, type, PageRequest.of(0, 50));
        } else if (direction != null) {
            transactions = walletTransactionRepository
                    .findAllByUser_IdAndDirectionOrderByCreatedAtDesc(userId, direction, PageRequest.of(0, 50));
        } else {
            transactions = walletTransactionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50));
        }
        return transactions.stream()
                .map(WalletTransactionResponse::from)
                .toList();
    }
}
