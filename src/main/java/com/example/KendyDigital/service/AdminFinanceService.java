package com.example.KendyDigital.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.config.BankProperties;
import com.example.KendyDigital.dto.AdminBankTransactionResponse;
import com.example.KendyDigital.dto.AdminDashboardResponse;
import com.example.KendyDigital.dto.AdminDepositCancelRequest;
import com.example.KendyDigital.dto.AdminDepositExtendRequest;
import com.example.KendyDigital.dto.AdminUserDetailResponse;
import com.example.KendyDigital.dto.AdminUserResponse;
import com.example.KendyDigital.dto.AdminUserRoleUpdateRequest;
import com.example.KendyDigital.dto.AdminUserStatusUpdateRequest;
import com.example.KendyDigital.dto.AdminWalletAdjustmentRequest;
import com.example.KendyDigital.dto.BulkManualCreditBankTransactionsRequest;
import com.example.KendyDigital.dto.DepositResponse;
import com.example.KendyDigital.dto.IgnoreBankTransactionRequest;
import com.example.KendyDigital.dto.ManualCreditBankTransactionRequest;
import com.example.KendyDigital.dto.ManualCreditDepositRequest;
import com.example.KendyDigital.dto.MatchBankTransactionRequest;
import com.example.KendyDigital.dto.ReprocessBankTransactionRequest;
import com.example.KendyDigital.dto.RevenueReportResponse;
import com.example.KendyDigital.dto.WalletTransactionResponse;
import com.example.KendyDigital.model.BankTransaction;
import com.example.KendyDigital.model.BankTransactionStatus;
import com.example.KendyDigital.model.DepositRequest;
import com.example.KendyDigital.model.DepositStatus;
import com.example.KendyDigital.model.OrderStatus;
import com.example.KendyDigital.model.TicketStatus;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.model.WalletTransaction;
import com.example.KendyDigital.model.WalletTransactionDirection;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.repository.BankTransactionRepository;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;

@Service
public class AdminFinanceService {
    private static final String TRANSFER_IN = "IN";

    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletLedgerService walletLedgerService;
    private final AuditService auditService;
    private final Pattern depositCodePattern;

    public AdminFinanceService(UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            TicketRepository ticketRepository,
            BankTransactionRepository bankTransactionRepository,
            DepositRequestRepository depositRequestRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletLedgerService walletLedgerService,
            AuditService auditService,
            BankProperties bankProperties) {
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletLedgerService = walletLedgerService;
        this.auditService = auditService;
        this.depositCodePattern = Pattern.compile("\\b" + Pattern.quote(bankProperties.getTransferPrefix())
                + "[A-Z0-9]{8,}\\b", Pattern.CASE_INSENSITIVE);
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                userAccountRepository.count(),
                userAccountRepository.countByStatus(UserStatus.ACTIVE),
                userAccountRepository.countByStatus(UserStatus.LOCKED),
                userAccountRepository.countByStatus(UserStatus.PENDING_VERIFY),
                userAccountRepository.sumAllBalances(),
                orderRepository.count(),
                orderRepository.countByStatus(OrderStatus.PROCESSING),
                orderRepository.countByStatus(OrderStatus.COMPLETED),
                orderRepository.countByStatus(OrderStatus.CANCELLED),
                orderRepository.countByStatus(OrderStatus.FAILED),
                orderRepository.countByStatus(OrderStatus.REFUNDED),
                depositRequestRepository.countByStatus(DepositStatus.PENDING),
                depositRequestRepository.countByStatus(DepositStatus.COMPLETED),
                depositRequestRepository.countByStatus(DepositStatus.MANUAL_REVIEW),
                depositRequestRepository.sumAmountByStatus(DepositStatus.COMPLETED),
                ticketRepository.countByStatus(TicketStatus.PENDING_ADMIN),
                ticketRepository.countByStatus(TicketStatus.PENDING_USER),
                ticketRepository.countByStatus(TicketStatus.RESOLVED),
                ticketRepository.countByStatus(TicketStatus.CLOSED));
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport() {
        BigDecimal depositVolume = walletTransactionRepository.sumAmountByTypeAndDirection(WalletTransactionType.DEPOSIT, WalletTransactionDirection.CREDIT);
        BigDecimal grossRevenue = orderRepository.sumAmountByStatus(OrderStatus.COMPLETED);
        BigDecimal totalRefunds = orderRepository.sumAmountByStatus(OrderStatus.REFUNDED);
        BigDecimal netRevenue = grossRevenue.subtract(totalRefunds);
        BigDecimal walletLiability = userAccountRepository.sumAllBalances();

        return new RevenueReportResponse(depositVolume, grossRevenue, totalRefunds, netRevenue, walletLiability);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(UserStatus status) {
        return listUsers(status, null);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(UserStatus status, Integer limit) {
        List<UserAccount> users = status == null
                ? userAccountRepository.findAllByOrderByCreatedAtDesc(page(limit))
                : userAccountRepository.findAllByStatusOrderByCreatedAtDesc(status, page(limit));
        return users.stream().map(AdminUserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> searchUsers(String query, UserStatus status, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        List<UserAccount> users = userAccountRepository.searchAdmin(
                normalizedQuery,
                parseLongOrNull(normalizedQuery),
                status,
                page(limit));
        return users.stream().map(AdminUserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new AdminUserDetailResponse(
                AdminUserResponse.from(user),
                orderRepository.countByUser_Id(userId),
                depositRequestRepository.countByUser_Id(userId),
                walletTransactionRepository.countByUser_Id(userId),
                ticketRepository.countByUser_Id(userId),
                depositRequestRepository.sumAmountByUserIdAndStatus(userId, DepositStatus.COMPLETED),
                walletTransactionRepository.sumAmountByUserIdAndType(userId, WalletTransactionType.PURCHASE),
                walletTransactionRepository.sumAmountByUserIdAndType(userId, WalletTransactionType.REFUND));
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long adminUserId, Long targetUserId, AdminUserStatusUpdateRequest request) {
        UserAccount user = userAccountRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setStatus(request.status());
        auditService.recordAdmin(
                adminUserId,
                "USER_STATUS_UPDATED",
                "USER",
                user.getId(),
                "status=" + request.status() + ",reason=" + blankToNull(request.reason()));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse updateUserRole(Long adminUserId, Long targetUserId, AdminUserRoleUpdateRequest request) {
        if (adminUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin cannot change own role");
        }
        UserAccount user = userAccountRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(request.role());
        auditService.recordAdmin(
                adminUserId,
                "USER_ROLE_UPDATED",
                "USER",
                user.getId(),
                "role=" + request.role() + ",reason=" + blankToNull(request.reason()));
        return AdminUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<DepositResponse> listDeposits(DepositStatus status, Long userId) {
        return listDeposits(status, userId, null);
    }

    @Transactional(readOnly = true)
    public List<DepositResponse> listDeposits(DepositStatus status, Long userId, Integer limit) {
        List<DepositRequest> deposits = userId != null
                ? depositRequestRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, page(limit))
                : status == null
                        ? depositRequestRepository.findAllByOrderByCreatedAtDesc(page(limit))
                        : depositRequestRepository.findAllByStatusOrderByCreatedAtDesc(status, page(limit));
        return deposits.stream().map(DepositResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DepositResponse> searchDeposits(String query, DepositStatus status, Long userId, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        List<DepositRequest> deposits = depositRequestRepository.searchAdmin(
                normalizedQuery,
                parseLongOrNull(normalizedQuery),
                status,
                userId,
                page(limit));
        return deposits.stream().map(DepositResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DepositResponse getDepositForAdmin(String depositCode) {
        return depositRequestRepository.findByDepositCode(normalizeDepositCode(depositCode))
                .map(DepositResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
    }

    @Transactional(readOnly = true)
    public List<AdminBankTransactionResponse> listBankTransactions(BankTransactionStatus status) {
        return listBankTransactions(status, null);
    }

    @Transactional(readOnly = true)
    public List<AdminBankTransactionResponse> listBankTransactions(BankTransactionStatus status, Integer limit) {
        List<BankTransaction> transactions = status == null
                ? bankTransactionRepository.findAllByOrderByReceivedAtDesc(page(limit))
                : bankTransactionRepository.findAllByStatusOrderByReceivedAtDesc(status, page(limit));
        return transactions.stream().map(AdminBankTransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBankTransactionResponse> searchBankTransactions(String query, BankTransactionStatus status,
            Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        Long parsedId = parseLongOrNull(normalizedQuery);
        List<BankTransaction> transactions = bankTransactionRepository.searchAdmin(
                normalizedQuery,
                parsedId,
                parsedId,
                status,
                page(limit));
        return transactions.stream().map(AdminBankTransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminBankTransactionResponse getBankTransaction(Long bankTransactionId) {
        return bankTransactionRepository.findById(bankTransactionId)
                .map(AdminBankTransactionResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank transaction not found"));
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listWalletTransactions(Long userId) {
        return listWalletTransactions(userId, null);
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listWalletTransactions(Long userId, Integer limit) {
        List<WalletTransaction> transactions = userId == null
                ? walletTransactionRepository.findAllByOrderByCreatedAtDesc(page(limit))
                : walletTransactionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, page(limit));
        return transactions.stream().map(WalletTransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> searchWalletTransactions(String query, Long userId,
            WalletTransactionType type, WalletTransactionDirection direction, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        List<WalletTransaction> transactions = walletTransactionRepository.searchAdmin(
                normalizedQuery,
                parseLongOrNull(normalizedQuery),
                userId,
                type,
                direction,
                page(limit));
        return transactions.stream().map(WalletTransactionResponse::from).toList();
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
        UserAccount user = userAccountRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

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
        return WalletTransactionResponse.from(transaction);
    }

    @Transactional
    public DepositResponse cancelDeposit(Long adminUserId, String depositCode, AdminDepositCancelRequest request) {
        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(normalizeDepositCode(depositCode))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
        ensureDepositCanBeChanged(deposit, "Completed deposit cannot be cancelled");
        if (deposit.getStatus() != DepositStatus.CANCELLED) {
            deposit.cancel();
            auditService.recordAdmin(
                    adminUserId,
                    "DEPOSIT_CANCELLED_BY_ADMIN",
                    "DEPOSIT_REQUEST",
                    deposit.getId(),
                    "reason=" + request.reason());
        }
        return DepositResponse.from(deposit);
    }

    @Transactional
    public DepositResponse extendDeposit(Long adminUserId, String depositCode, AdminDepositExtendRequest request) {
        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(normalizeDepositCode(depositCode))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
        ensureDepositCanBeChanged(deposit, "Completed deposit cannot be extended");
        if (deposit.getStatus() == DepositStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled deposit cannot be extended");
        }

        Instant baseTime = deposit.getExpiredAt().isAfter(Instant.now()) ? deposit.getExpiredAt() : Instant.now();
        deposit.extendTo(baseTime.plusSeconds(request.minutes() * 60L));
        auditService.recordAdmin(
                adminUserId,
                "DEPOSIT_EXTENDED",
                "DEPOSIT_REQUEST",
                deposit.getId(),
                "minutes=" + request.minutes() + ",reason=" + request.reason());
        return DepositResponse.from(deposit);
    }

    @Transactional
    public DepositResponse manualCreditDeposit(Long adminUserId, String depositCode,
            ManualCreditDepositRequest request) {
        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(normalizeDepositCode(depositCode))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
        ensureDepositCanBeChanged(deposit, "Deposit has already been credited");
        if (deposit.getStatus() == DepositStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled deposit cannot be credited");
        }
        if (deposit.getMatchedBankTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Use bank transaction manual-credit for deposits with a matched bank transaction");
        }

        UserAccount user = userAccountRepository.findByIdForUpdate(deposit.getUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
        }

        WalletTransaction walletTransaction = walletLedgerService.credit(
                user,
                deposit.getAmount(),
                WalletTransactionType.DEPOSIT,
                "DEPOSIT_REQUEST",
                deposit.getId(),
                "Manual deposit credit " + deposit.getDepositCode() + ": " + request.reason(),
                adminUserId);
        deposit.complete(null, walletTransaction);

        auditService.recordAdmin(
                adminUserId,
                "DEPOSIT_MANUAL_CREDITED",
                "DEPOSIT_REQUEST",
                deposit.getId(),
                "userId=" + user.getId() + ",walletTransactionId=" + walletTransaction.getId()
                        + ",reason=" + request.reason());
        return DepositResponse.from(deposit);
    }

    @Transactional
    public AdminBankTransactionResponse ignoreBankTransaction(Long adminUserId, Long bankTransactionId,
            IgnoreBankTransactionRequest request) {
        BankTransaction transaction = bankTransactionRepository.findByIdForUpdate(bankTransactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank transaction not found"));
        ensureNotCredited(transaction);

        transaction.ignore(request.reason());
        auditService.recordAdmin(
                adminUserId,
                "BANK_TRANSACTION_IGNORED",
                "BANK_TRANSACTION",
                transaction.getId(),
                "reason=" + request.reason());
        return AdminBankTransactionResponse.from(transaction);
    }

    @Transactional
    public AdminBankTransactionResponse matchBankTransaction(Long adminUserId, Long bankTransactionId,
            MatchBankTransactionRequest request) {
        BankTransaction bankTransaction = bankTransactionRepository.findByIdForUpdate(bankTransactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank transaction not found"));
        ensureNotCredited(bankTransaction);
        ensureNotDuplicate(bankTransaction);
        ensureIncomingTransfer(bankTransaction);

        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(
                        normalizeDepositCode(request.depositCode()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
        ensureDepositCanBeLinked(deposit);

        if (deposit.getStatus() == DepositStatus.PENDING && deposit.getExpiredAt().isBefore(Instant.now())) {
            deposit.markManualReview();
        }
        if (deposit.getStatus() == DepositStatus.PENDING
                && !amountMatches(deposit.getAmount(), bankTransaction.getTransferAmount())) {
            deposit.markManualReview();
        }

        bankTransaction.match(deposit, request.reason());
        auditService.recordAdmin(
                adminUserId,
                "BANK_TRANSACTION_MATCHED",
                "BANK_TRANSACTION",
                bankTransaction.getId(),
                "depositCode=" + deposit.getDepositCode() + ",reason=" + request.reason());
        return AdminBankTransactionResponse.from(bankTransaction);
    }

    @Transactional
    public AdminBankTransactionResponse reprocessBankTransaction(Long adminUserId, Long bankTransactionId,
            ReprocessBankTransactionRequest request) {
        BankTransaction bankTransaction = bankTransactionRepository.findByIdForUpdate(bankTransactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank transaction not found"));
        ensureNotCredited(bankTransaction);
        ensureNotDuplicate(bankTransaction);

        if (!isIncomingTransfer(bankTransaction)) {
            bankTransaction.ignore("Reprocess ignored non-incoming transfer: " + request.reason());
            auditService.recordAdmin(
                    adminUserId,
                    "BANK_TRANSACTION_REPROCESS_IGNORED",
                    "BANK_TRANSACTION",
                    bankTransaction.getId(),
                    "reason=" + request.reason());
            return AdminBankTransactionResponse.from(bankTransaction);
        }

        BigDecimal transferAmount = bankTransaction.getTransferAmount();
        if (transferAmount == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Bank transaction amount is invalid",
                    null,
                    request.reason());
        }

        DepositRequest deposit = resolveDepositForReprocess(bankTransaction, request.depositCode()).orElse(null);
        if (deposit == null) {
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Deposit request not found",
                    null,
                    request.reason());
        }

        if (deposit.getStatus() == DepositStatus.COMPLETED || deposit.getWalletTransaction() != null) {
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Deposit request has already been credited",
                    deposit,
                    request.reason());
        }
        if (deposit.getStatus() == DepositStatus.CANCELLED) {
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Deposit request has been cancelled",
                    deposit,
                    request.reason());
        }
        if (deposit.getExpiredAt().isBefore(Instant.now())) {
            deposit.markManualReview();
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Deposit request expired",
                    deposit,
                    request.reason());
        }
        if (!amountMatches(deposit.getAmount(), transferAmount)) {
            deposit.markManualReview();
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Transfer amount does not match deposit request",
                    deposit,
                    request.reason());
        }

        UserAccount user = userAccountRepository.findByIdForUpdate(deposit.getUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "User is not active",
                    deposit,
                    request.reason());
        }

        WalletTransaction walletTransaction = walletLedgerService.credit(
                user,
                transferAmount,
                WalletTransactionType.DEPOSIT,
                "DEPOSIT_REQUEST",
                deposit.getId(),
                "Reprocessed bank deposit " + bankTransaction.getId() + " for " + deposit.getDepositCode(),
                adminUserId);
        deposit.complete(bankTransaction, walletTransaction);
        bankTransaction.credit(deposit, walletTransaction);

        auditService.recordAdmin(
                adminUserId,
                "BANK_TRANSACTION_REPROCESSED_CREDITED",
                "BANK_TRANSACTION",
                bankTransaction.getId(),
                "depositCode=" + deposit.getDepositCode() + ",walletTransactionId=" + walletTransaction.getId()
                        + ",reason=" + request.reason());
        return AdminBankTransactionResponse.from(bankTransaction);
    }

    @Transactional
    public AdminBankTransactionResponse manualCreditBankTransaction(Long adminUserId, Long bankTransactionId,
            ManualCreditBankTransactionRequest request) {
        BankTransaction bankTransaction = bankTransactionRepository.findByIdForUpdate(bankTransactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank transaction not found"));
        ensureNotCredited(bankTransaction);
        ensureIncomingTransfer(bankTransaction);

        BigDecimal transferAmount = bankTransaction.getTransferAmount();
        if (transferAmount == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank transaction amount is invalid");
        }

        UserAccount user = userAccountRepository.findByIdForUpdate(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
        }

        DepositRequest depositRequest = findOptionalDepositForManualCredit(request.depositCode(), user);
        WalletTransaction walletTransaction = walletLedgerService.credit(
                user,
                transferAmount,
                WalletTransactionType.DEPOSIT,
                "BANK_TRANSACTION",
                bankTransaction.getId(),
                "Manual bank credit " + bankTransaction.getId() + ": " + request.reason(),
                adminUserId);

        if (depositRequest != null) {
            depositRequest.complete(bankTransaction, walletTransaction);
        }
        bankTransaction.creditManually(user, depositRequest, walletTransaction, request.reason());

        auditService.recordAdmin(
                adminUserId,
                "BANK_TRANSACTION_MANUAL_CREDITED",
                "BANK_TRANSACTION",
                bankTransaction.getId(),
                "userId=" + user.getId() + ",walletTransactionId=" + walletTransaction.getId()
                        + ",reason=" + request.reason());
        return AdminBankTransactionResponse.from(bankTransaction);
    }

    @Transactional
    public List<AdminBankTransactionResponse> bulkManualCreditBankTransactions(Long adminUserId,
            BulkManualCreditBankTransactionsRequest request) {
        return request.bankTransactionIds().stream()
                .map(id -> manualCreditBankTransaction(adminUserId, id,
                        new ManualCreditBankTransactionRequest(request.userId(), request.depositCode(),
                                request.reason())))
                .toList();
    }

    private DepositRequest findOptionalDepositForManualCredit(String depositCode, UserAccount user) {
        if (depositCode == null || depositCode.isBlank()) {
            return null;
        }

        DepositRequest depositRequest = depositRequestRepository
                .findByDepositCodeForUpdate(depositCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
        if (!depositRequest.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit request belongs to another user");
        }
        if (depositRequest.getStatus() == DepositStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit request has already been completed");
        }
        if (depositRequest.getStatus() == DepositStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit request has been cancelled");
        }
        return depositRequest;
    }

    private Optional<DepositRequest> resolveDepositForReprocess(BankTransaction bankTransaction, String depositCode) {
        if (depositCode != null && !depositCode.isBlank()) {
            return depositRequestRepository.findByDepositCodeForUpdate(normalizeDepositCode(depositCode));
        }

        if (bankTransaction.getMatchedDepositRequest() != null) {
            return depositRequestRepository.findByDepositCodeForUpdate(
                    bankTransaction.getMatchedDepositRequest().getDepositCode());
        }

        return extractDepositCode(bankTransaction.getContent(), bankTransaction.getCode())
                .flatMap(depositRequestRepository::findByDepositCodeForUpdate);
    }

    private AdminBankTransactionResponse markManualReviewAfterReprocess(Long adminUserId,
            BankTransaction bankTransaction, String reviewReason, DepositRequest deposit, String adminReason) {
        bankTransaction.manualReview(reviewReason + ": " + adminReason, deposit);
        auditService.recordAdmin(
                adminUserId,
                "BANK_TRANSACTION_REPROCESS_MANUAL_REVIEW",
                "BANK_TRANSACTION",
                bankTransaction.getId(),
                "reviewReason=" + reviewReason + ",adminReason=" + adminReason
                        + (deposit == null ? "" : ",depositCode=" + deposit.getDepositCode()));
        return AdminBankTransactionResponse.from(bankTransaction);
    }

    private void ensureNotCredited(BankTransaction transaction) {
        if (transaction.getStatus() == BankTransactionStatus.CREDITED || transaction.getWalletTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bank transaction has already been credited");
        }
    }

    private void ensureNotDuplicate(BankTransaction transaction) {
        if (transaction.getStatus() == BankTransactionStatus.DUPLICATE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate bank transaction cannot be reprocessed");
        }
    }

    private void ensureIncomingTransfer(BankTransaction transaction) {
        if (!isIncomingTransfer(transaction)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only incoming bank transactions can be credited");
        }
    }

    private boolean isIncomingTransfer(BankTransaction transaction) {
        return transaction.getTransferType() == null || TRANSFER_IN.equalsIgnoreCase(transaction.getTransferType());
    }

    private void ensureDepositCanBeChanged(DepositRequest deposit, String message) {
        if (deposit.getStatus() == DepositStatus.COMPLETED || deposit.getWalletTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private void ensureDepositCanBeLinked(DepositRequest deposit) {
        if (deposit.getStatus() == DepositStatus.COMPLETED || deposit.getWalletTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit request has already been credited");
        }
        if (deposit.getStatus() == DepositStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit request has been cancelled");
        }
    }

    private boolean amountMatches(BigDecimal expectedAmount, BigDecimal actualAmount) {
        return expectedAmount != null && actualAmount != null && expectedAmount.compareTo(actualAmount) == 0;
    }

    private Optional<String> extractDepositCode(String content, String code) {
        String text = (nullSafe(content) + " " + nullSafe(code)).toUpperCase(Locale.ROOT);
        Matcher matcher = depositCodePattern.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group().toUpperCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        return PageRequest.of(0, normalizedLimit);
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? null : query.trim();
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

    private String normalizeDepositCode(String depositCode) {
        if (depositCode == null || depositCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deposit code is required");
        }
        return depositCode.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
