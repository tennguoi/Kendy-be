package com.example.KendyDigital.controller;
import com.example.KendyDigital.dto.finance.response.RevenueReportResponse;


import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.finance.response.AdminBankTransactionResponse;
import com.example.KendyDigital.dto.finance.response.AdminDashboardResponse;
import com.example.KendyDigital.dto.finance.request.AdminDepositCancelRequest;
import com.example.KendyDigital.dto.finance.request.AdminDepositExtendRequest;
import com.example.KendyDigital.dto.user.response.AdminUserDetailResponse;
import com.example.KendyDigital.dto.user.response.AdminUserResponse;
import com.example.KendyDigital.dto.user.request.AdminUserRoleUpdateRequest;
import com.example.KendyDigital.dto.user.request.AdminUserStatusUpdateRequest;
import com.example.KendyDigital.dto.user.request.AdminWalletAdjustmentRequest;
import com.example.KendyDigital.dto.finance.response.BalanceIntegrityIssueResponse;
import com.example.KendyDigital.dto.finance.request.BulkManualCreditBankTransactionsRequest;
import com.example.KendyDigital.dto.deposit.response.DepositResponse;
import com.example.KendyDigital.dto.finance.request.IgnoreBankTransactionRequest;
import com.example.KendyDigital.dto.finance.request.ManualCreditBankTransactionRequest;
import com.example.KendyDigital.dto.finance.request.ManualCreditDepositRequest;
import com.example.KendyDigital.dto.finance.request.MatchBankTransactionRequest;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.dto.finance.request.ReprocessBankTransactionRequest;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import com.example.KendyDigital.model.BankTransactionStatus;
import com.example.KendyDigital.model.DepositStatus;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.model.WalletTransactionDirection;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.AdminUserManagerService;
import com.example.KendyDigital.service.AdminDepositManagerService;
import com.example.KendyDigital.service.AdminBankTxManagerService;
import com.example.KendyDigital.service.AdminWalletManagerService;
import com.example.KendyDigital.service.AdminOrderManagerService;
import com.example.KendyDigital.service.AdminFinanceReportService;
import com.example.KendyDigital.service.BalanceIntegrityService;

import jakarta.validation.Valid;

@RestController
public class AdminFinanceController {
    private final AdminUserManagerService userManagerService;
    private final AdminDepositManagerService depositManagerService;
    private final AdminBankTxManagerService bankTxManagerService;
    private final AdminWalletManagerService walletManagerService;
    private final AdminOrderManagerService orderManagerService;
    private final AdminFinanceReportService financeReportService;
    private final BalanceIntegrityService balanceIntegrityService;

    public AdminFinanceController(
            AdminUserManagerService userManagerService,
            AdminDepositManagerService depositManagerService,
            AdminBankTxManagerService bankTxManagerService,
            AdminWalletManagerService walletManagerService,
            AdminOrderManagerService orderManagerService,
            AdminFinanceReportService financeReportService,
            BalanceIntegrityService balanceIntegrityService) {
        this.userManagerService = userManagerService;
        this.depositManagerService = depositManagerService;
        this.bankTxManagerService = bankTxManagerService;
        this.walletManagerService = walletManagerService;
        this.orderManagerService = orderManagerService;
        this.financeReportService = financeReportService;
        this.balanceIntegrityService = balanceIntegrityService;
    }

    @GetMapping("/api/admin/dashboard")
    public AdminDashboardResponse dashboard() {
        return financeReportService.dashboard();
    }

    @GetMapping("/api/admin/users")
    public List<AdminUserResponse> listUsers(@RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userManagerService.listUsers(status, page, size);
    }

    @GetMapping("/api/admin/users/search")
    public List<AdminUserResponse> searchUsers(@RequestParam(required = false) String query,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userManagerService.searchUsers(query, status, page, size);
    }

    @GetMapping("/api/admin/users/{userId}")
    public AdminUserDetailResponse getUserDetail(@PathVariable Long userId) {
        return userManagerService.getUserDetail(userId);
    }

    @PatchMapping("/api/admin/users/{userId}/status")
    public AdminUserResponse updateUserStatus(Authentication authentication, @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request) {
        return userManagerService.updateUserStatus(CurrentUser.require(authentication).userId(), userId, request);
    }

    @PatchMapping("/api/admin/users/{userId}/role")
    public AdminUserResponse updateUserRole(Authentication authentication, @PathVariable Long userId,
            @Valid @RequestBody AdminUserRoleUpdateRequest request) {
        return userManagerService.updateUserRole(CurrentUser.require(authentication).userId(), userId, request);
    }

    @PostMapping("/api/admin/users/{userId}/wallet-adjustments")
    public WalletTransactionResponse adjustWallet(Authentication authentication, @PathVariable Long userId,
            @Valid @RequestBody AdminWalletAdjustmentRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return walletManagerService.adjustWallet(adminUserId, userId, request);
    }

    @GetMapping("/api/admin/deposit-requests")
    public List<DepositResponse> listDeposits(@RequestParam(required = false) DepositStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return depositManagerService.listDeposits(status, userId, parseInstant(fromDate), parseInstant(toDate), page, size);
    }

    @GetMapping("/api/admin/deposit-requests/search")
    public List<DepositResponse> searchDeposits(@RequestParam(required = false) String query,
            @RequestParam(required = false) DepositStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return depositManagerService.searchDeposits(query, status, userId, parseInstant(fromDate), parseInstant(toDate), page, size);
    }

    @GetMapping("/api/admin/deposit-requests/{depositCode}")
    public DepositResponse getDeposit(@PathVariable String depositCode) {
        return depositManagerService.getDepositForAdmin(depositCode);
    }

    @GetMapping("/api/admin/deposit-requests/expired")
    public List<DepositResponse> listExpiredDeposits(@RequestParam(required = false) Integer limit) {
        return depositManagerService.listDeposits(DepositStatus.EXPIRED, null, limit);
    }

    @GetMapping("/api/admin/deposit-requests/manual-review")
    public List<DepositResponse> listManualReviewDeposits(@RequestParam(required = false) Integer limit) {
        return depositManagerService.listDeposits(DepositStatus.MANUAL_REVIEW, null, limit);
    }

    @PostMapping("/api/admin/deposit-requests/{depositCode}/cancel")
    public DepositResponse cancelDeposit(Authentication authentication, @PathVariable String depositCode,
            @Valid @RequestBody AdminDepositCancelRequest request) {
        return depositManagerService.cancelDeposit(CurrentUser.require(authentication).userId(), depositCode, request);
    }

    @PostMapping("/api/admin/deposit-requests/{depositCode}/extend")
    public DepositResponse extendDeposit(Authentication authentication, @PathVariable String depositCode,
            @Valid @RequestBody AdminDepositExtendRequest request) {
        return depositManagerService.extendDeposit(CurrentUser.require(authentication).userId(), depositCode, request);
    }

    @PostMapping("/api/admin/deposit-requests/{depositCode}/manual-credit")
    public DepositResponse manualCreditDeposit(Authentication authentication, @PathVariable String depositCode,
            @Valid @RequestBody ManualCreditDepositRequest request) {
        return depositManagerService.manualCreditDeposit(CurrentUser.require(authentication).userId(), depositCode, request);
    }

    @GetMapping("/api/admin/bank-transactions")
    public List<AdminBankTransactionResponse> listBankTransactions(
            @RequestParam(required = false) BankTransactionStatus status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bankTxManagerService.listBankTransactions(status, parseInstant(fromDate), parseInstant(toDate), page, size);
    }

    @GetMapping("/api/admin/bank-transactions/search")
    public List<AdminBankTransactionResponse> searchBankTransactions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BankTransactionStatus status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bankTxManagerService.searchBankTransactions(query, status, parseInstant(fromDate), parseInstant(toDate), page, size);
    }

    @GetMapping("/api/admin/bank-transactions/{id}")
    public AdminBankTransactionResponse getBankTransaction(@PathVariable Long id) {
        return bankTxManagerService.getBankTransaction(id);
    }

    @GetMapping("/api/admin/bank-transactions/manual-review")
    public List<AdminBankTransactionResponse> listManualReviewBankTransactions(
            @RequestParam(required = false) Integer limit) {
        return bankTxManagerService.listBankTransactions(BankTransactionStatus.MANUAL_REVIEW, limit);
    }

    @GetMapping("/api/admin/bank-transactions/duplicate")
    public List<AdminBankTransactionResponse> listDuplicateBankTransactions(
            @RequestParam(required = false) Integer limit) {
        return bankTxManagerService.listBankTransactions(BankTransactionStatus.DUPLICATE, limit);
    }

    @GetMapping("/api/admin/bank-transactions/ignored")
    public List<AdminBankTransactionResponse> listIgnoredBankTransactions(
            @RequestParam(required = false) Integer limit) {
        return bankTxManagerService.listBankTransactions(BankTransactionStatus.IGNORED, limit);
    }

    @PostMapping("/api/admin/bank-transactions/{id}/manual-credit")
    public AdminBankTransactionResponse manualCreditBankTransaction(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody ManualCreditBankTransactionRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return bankTxManagerService.manualCreditBankTransaction(adminUserId, id, request);
    }

    @PostMapping("/api/admin/bank-transactions/bulk-manual-credit")
    public List<AdminBankTransactionResponse> bulkManualCreditBankTransactions(Authentication authentication,
            @Valid @RequestBody BulkManualCreditBankTransactionsRequest request) {
        return bankTxManagerService.bulkManualCreditBankTransactions(CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/admin/bank-transactions/{id}/match")
    public AdminBankTransactionResponse matchBankTransaction(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody MatchBankTransactionRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return bankTxManagerService.matchBankTransaction(adminUserId, id, request);
    }

    @PostMapping("/api/admin/bank-transactions/{id}/reprocess")
    public AdminBankTransactionResponse reprocessBankTransaction(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody ReprocessBankTransactionRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return bankTxManagerService.reprocessBankTransaction(adminUserId, id, request);
    }

    @PostMapping("/api/admin/bank-transactions/{id}/ignore")
    public AdminBankTransactionResponse ignoreBankTransaction(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody IgnoreBankTransactionRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return bankTxManagerService.ignoreBankTransaction(adminUserId, id, request);
    }

    @GetMapping("/api/admin/wallet-transactions")
    public List<WalletTransactionResponse> listWalletTransactions(@RequestParam(required = false) Long userId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return walletManagerService.listWalletTransactions(userId, parseInstant(fromDate), parseInstant(toDate), page, size);
    }

    @GetMapping("/api/admin/wallet-transactions/search")
    public List<WalletTransactionResponse> searchWalletTransactions(@RequestParam(required = false) String query,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) WalletTransactionType type,
            @RequestParam(required = false) WalletTransactionDirection direction,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return walletManagerService.searchWalletTransactions(query, userId, type, direction, parseInstant(fromDate), parseInstant(toDate), page, size);
    }

    @GetMapping("/api/admin/wallet-transactions/{id}/details")
    public WalletTransactionResponse getWalletTransaction(@PathVariable Long id) {
        return walletManagerService.getWalletTransaction(id);
    }

    @PostMapping("/api/admin/wallet-transactions/reconciliation")
    public List<BalanceIntegrityIssueResponse> reconcileWalletTransactions() {
        return balanceIntegrityService.findIssues();
    }

    @GetMapping("/api/admin/wallet-transactions/balance-check")
    public List<BalanceIntegrityIssueResponse> balanceCheck() {
        return balanceIntegrityService.findIssues();
    }

    @GetMapping("/api/admin/reports/balance-integrity")
    public List<BalanceIntegrityIssueResponse> balanceIntegrityIssues() {
        return balanceIntegrityService.findIssues();
    }

    @GetMapping("/api/admin/orders")
    public List<OrderResponse> listOrders(@RequestParam(required = false) com.example.KendyDigital.model.OrderStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderManagerService.listOrders(status, userId, parseInstant(fromDate), parseInstant(toDate), page, size);
    }

    @GetMapping("/api/admin/orders/search")
    public List<OrderResponse> searchOrders(@RequestParam(required = false) String query,
            @RequestParam(required = false) com.example.KendyDigital.model.OrderStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderManagerService.searchOrders(query, status, userId, parseInstant(fromDate), parseInstant(toDate), page, size);
    }

    @GetMapping("/api/admin/reports/revenue")
    public com.example.KendyDigital.dto.finance.response.RevenueReportResponse getRevenueReport(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return financeReportService.getRevenueReport(parseInstant(fromDate), parseInstant(toDate));
    }

    private Instant parseInstant(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
