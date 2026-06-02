package com.example.KendyDigital.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.AdminBankTransactionResponse;
import com.example.KendyDigital.dto.AdminDashboardResponse;
import com.example.KendyDigital.dto.AdminUserDetailResponse;
import com.example.KendyDigital.dto.AdminUserResponse;
import com.example.KendyDigital.dto.AdminUserRoleUpdateRequest;
import com.example.KendyDigital.dto.AdminUserStatusUpdateRequest;
import com.example.KendyDigital.dto.AdminWalletAdjustmentRequest;
import com.example.KendyDigital.dto.DepositResponse;
import com.example.KendyDigital.dto.IgnoreBankTransactionRequest;
import com.example.KendyDigital.dto.ManualCreditBankTransactionRequest;
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

    public AdminFinanceService(UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            TicketRepository ticketRepository,
            BankTransactionRepository bankTransactionRepository,
            DepositRequestRepository depositRequestRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletLedgerService walletLedgerService,
            AuditService auditService) {
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletLedgerService = walletLedgerService;
        this.auditService = auditService;
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
        List<UserAccount> users = status == null
                ? userAccountRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100))
                : userAccountRepository.findAllByStatusOrderByCreatedAtDesc(status, PageRequest.of(0, 100));
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
        List<DepositRequest> deposits = userId != null
                ? depositRequestRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100))
                : status == null
                        ? depositRequestRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100))
                        : depositRequestRepository.findAllByStatusOrderByCreatedAtDesc(status, PageRequest.of(0, 100));
        return deposits.stream().map(DepositResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBankTransactionResponse> listBankTransactions(BankTransactionStatus status) {
        List<BankTransaction> transactions = status == null
                ? bankTransactionRepository.findAllByOrderByReceivedAtDesc(PageRequest.of(0, 100))
                : bankTransactionRepository.findAllByStatusOrderByReceivedAtDesc(status, PageRequest.of(0, 100));
        return transactions.stream().map(AdminBankTransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listWalletTransactions(Long userId) {
        List<WalletTransaction> transactions = userId == null
                ? walletTransactionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100))
                : walletTransactionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100));
        return transactions.stream().map(WalletTransactionResponse::from).toList();
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

    private void ensureNotCredited(BankTransaction transaction) {
        if (transaction.getStatus() == BankTransactionStatus.CREDITED || transaction.getWalletTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bank transaction has already been credited");
        }
    }

    private void ensureIncomingTransfer(BankTransaction transaction) {
        if (transaction.getTransferType() != null && !TRANSFER_IN.equalsIgnoreCase(transaction.getTransferType())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only incoming bank transactions can be credited");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
