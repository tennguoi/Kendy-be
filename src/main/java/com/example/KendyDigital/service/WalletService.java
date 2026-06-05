package com.example.KendyDigital.service;

import java.util.List;
import java.util.Locale;

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
        return listTransactions(userId, type, direction, 0, 50);
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listTransactions(Long userId, WalletTransactionType type,
            WalletTransactionDirection direction, int page, int size) {
        List<WalletTransaction> transactions;
        if (type != null && direction != null) {
            transactions = walletTransactionRepository
                    .findAllByUser_IdAndTypeAndDirectionOrderByCreatedAtDesc(userId, type, direction, paged(page, size));
        } else if (type != null) {
            transactions = walletTransactionRepository.findAllByUser_IdAndTypeOrderByCreatedAtDesc(userId, type, paged(page, size));
        } else if (direction != null) {
            transactions = walletTransactionRepository
                    .findAllByUser_IdAndDirectionOrderByCreatedAtDesc(userId, direction, paged(page, size));
        } else {
            transactions = walletTransactionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size));
        }
        return transactions.stream()
                .map(WalletTransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> searchTransactions(Long userId, String query, WalletTransactionType type,
            WalletTransactionDirection direction, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        return walletTransactionRepository.searchUser(
                        userId,
                        likePattern(normalizedQuery),
                        parseLongOrNull(normalizedQuery),
                        type,
                        direction,
                        paged(page, size))
                .stream()
                .map(WalletTransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WalletTransactionResponse getTransaction(Long userId, Long id) {
        return walletTransactionRepository.findByIdAndUser_Id(id, userId)
                .map(WalletTransactionResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet transaction not found"));
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }

    private String normalizeQuery(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String likePattern(String value) {
        return value == null ? null : "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    private Long parseLongOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
