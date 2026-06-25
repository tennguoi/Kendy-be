package com.example.KendyDigital.service.wallet;

import com.example.KendyDigital.dto.user.request.AdminWalletAdjustmentRequest;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.model.wallet.WalletTransaction;
import com.example.KendyDigital.model.wallet.WalletTransactionDirection;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminWalletManagerServiceImpl  implements AdminWalletManagerService{
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserAccountRepository userAccountRepository;
    private final WalletLedgerService walletLedgerService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final UserNotificationService userNotificationService;
    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("1000");

    public AdminWalletManagerServiceImpl(WalletTransactionRepository walletTransactionRepository,
            UserAccountRepository userAccountRepository,
            WalletLedgerService walletLedgerService,
            AuditService auditService,
            PasswordEncoder passwordEncoder,
            UserNotificationService userNotificationService) {
        this.walletTransactionRepository = walletTransactionRepository;
        this.userAccountRepository = userAccountRepository;
        this.walletLedgerService = walletLedgerService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.userNotificationService = userNotificationService;
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listWalletTransactions(Long userId) {
        return listWalletTransactions(userId, null);
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listWalletTransactions(Long userId,
            Instant fromDate, Instant toDate, int page, int size) {
        List<WalletTransaction> transactions = userId == null
                ? walletTransactionRepository.findAllByOrderByCreatedAtDesc(paged(page, size))
                : walletTransactionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size));
        return transactions.stream().map(WalletTransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listWalletTransactions(Long userId, Integer limit) {
        return listWalletTransactions(userId, null, null, 0, limit == null ? 100 : limit);
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> searchWalletTransactions(String query, Long userId,
            WalletTransactionType type, WalletTransactionDirection direction,
            Instant fromDate, Instant toDate, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        List<WalletTransaction> transactions = walletTransactionRepository.searchAdmin(
                likePattern(normalizedQuery),
                parseLongOrNull(normalizedQuery),
                userId,
                type,
                direction,
                paged(page, size));
        return transactions.stream().map(WalletTransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> searchWalletTransactions(String query, Long userId,
            WalletTransactionType type, WalletTransactionDirection direction, Integer limit) {
        return searchWalletTransactions(query, userId, type, direction, null, null, 0, limit == null ? 100 : limit);
    }

    @Transactional(readOnly = true)
    public WalletTransactionResponse getWalletTransaction(Long walletTransactionId) {
        return walletTransactionRepository.findById(walletTransactionId)
                .map(WalletTransactionResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet transaction not found"));
    }

    @Transactional
    public WalletTransactionResponse adjustWallet(Long adminUserId, Long targetUserId,
            AdminWalletAdjustmentRequest request) {
        UserAccount admin = userAccountRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (admin.getRole() != UserRole.ADMIN && admin.getRole() != UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
        UserAccount user = userAccountRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.amount().compareTo(LARGE_TRANSACTION_THRESHOLD) >= 0) {
            if (request.confirmationPassword() == null || request.confirmationPassword().isBlank()
                    || !passwordEncoder.matches(request.confirmationPassword(), admin.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Confirmation password required for transactions of this size");
            }
        }

        WalletTransaction transaction;
        if (request.direction() == WalletTransactionDirection.CREDIT) {
            transaction = walletLedgerService.credit(
                    user,
                    request.amount(),
                    WalletTransactionType.ADMIN_ADJUST,
                    "ADMIN_WALLET_ADJUSTMENT",
                    user.getId(),
                    request.reason(),
                    adminUserId);
        } else if (request.direction() == WalletTransactionDirection.DEBIT) {
            transaction = walletLedgerService.debit(
                    user,
                    request.amount(),
                    WalletTransactionType.ADMIN_ADJUST,
                    "ADMIN_WALLET_ADJUSTMENT",
                    user.getId(),
                    request.reason(),
                    adminUserId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported wallet adjustment direction");
        }

        auditService.recordAdmin(
                adminUserId,
                "ADMIN_WALLET_ADJUSTED",
                "USER",
                user.getId(),
                "direction=" + request.direction() + ",amount=" + request.amount() + ",reason=" + request.reason());
        userNotificationService.create(user.getId(), "Wallet adjusted",
            "Your wallet has been " + (request.direction() == WalletTransactionDirection.CREDIT ? "credited" : "debited") + " " + request.amount() + " VND. Reason: " + request.reason(),
            "WALLET", "/wallet");
        return WalletTransactionResponse.from(transaction);
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? null : query.trim();
    }

    private String likePattern(String query) {
        return query == null ? null : "%" + query.toLowerCase(Locale.ROOT) + "%";
    }

    private Long parseLongOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
