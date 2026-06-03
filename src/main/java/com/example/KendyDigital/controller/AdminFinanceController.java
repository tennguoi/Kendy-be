package com.example.KendyDigital.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.AdminBankTransactionResponse;
import com.example.KendyDigital.dto.AdminDashboardResponse;
import com.example.KendyDigital.dto.AdminDepositCancelRequest;
import com.example.KendyDigital.dto.AdminDepositExtendRequest;
import com.example.KendyDigital.dto.AdminUserDetailResponse;
import com.example.KendyDigital.dto.AdminUserResponse;
import com.example.KendyDigital.dto.AdminUserRoleUpdateRequest;
import com.example.KendyDigital.dto.AdminUserStatusUpdateRequest;
import com.example.KendyDigital.dto.AdminWalletAdjustmentRequest;
import com.example.KendyDigital.dto.BalanceIntegrityIssueResponse;
import com.example.KendyDigital.dto.BulkManualCreditBankTransactionsRequest;
import com.example.KendyDigital.dto.DepositResponse;
import com.example.KendyDigital.dto.IgnoreBankTransactionRequest;
import com.example.KendyDigital.dto.ManualCreditBankTransactionRequest;
import com.example.KendyDigital.dto.ManualCreditDepositRequest;
import com.example.KendyDigital.dto.MatchBankTransactionRequest;
import com.example.KendyDigital.dto.ReprocessBankTransactionRequest;
import com.example.KendyDigital.dto.WalletTransactionResponse;
import com.example.KendyDigital.model.BankTransactionStatus;
import com.example.KendyDigital.model.DepositStatus;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.model.WalletTransactionDirection;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.AdminFinanceService;
import com.example.KendyDigital.service.BalanceIntegrityService;

import jakarta.validation.Valid;

@RestController
public class AdminFinanceController {
    private final AdminFinanceService adminFinanceService;
    private final BalanceIntegrityService balanceIntegrityService;

    public AdminFinanceController(AdminFinanceService adminFinanceService,
            BalanceIntegrityService balanceIntegrityService) {
        this.adminFinanceService = adminFinanceService;
        this.balanceIntegrityService = balanceIntegrityService;
    }

    @GetMapping("/api/admin/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminFinanceService.dashboard();
    }

    @GetMapping("/api/admin/users")
    public List<AdminUserResponse> listUsers(@RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Integer limit) {
        return adminFinanceService.listUsers(status, limit);
    }

    @GetMapping("/api/admin/users/search")
    public List<AdminUserResponse> searchUsers(@RequestParam(required = false) String query,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Integer limit) {
        return adminFinanceService.searchUsers(query, status, limit);
    }

    @GetMapping("/api/admin/users/{userId}")
    public AdminUserDetailResponse getUserDetail(@PathVariable Long userId) {
        return adminFinanceService.getUserDetail(userId);
    }

    @PatchMapping("/api/admin/users/{userId}/status")
    public AdminUserResponse updateUserStatus(Authentication authentication, @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request) {
        return adminFinanceService.updateUserStatus(CurrentUser.require(authentication).userId(), userId, request);
    }

    @PatchMapping("/api/admin/users/{userId}/role")
    public AdminUserResponse updateUserRole(Authentication authentication, @PathVariable Long userId,
            @Valid @RequestBody AdminUserRoleUpdateRequest request) {
        return adminFinanceService.updateUserRole(CurrentUser.require(authentication).userId(), userId, request);
    }

    @PostMapping("/api/admin/users/{userId}/wallet-adjustments")
    public WalletTransactionResponse adjustWallet(Authentication authentication, @PathVariable Long userId,
            @Valid @RequestBody AdminWalletAdjustmentRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return adminFinanceService.adjustWallet(adminUserId, userId, request);
    }

    @GetMapping("/api/admin/deposit-requests")
    public List<DepositResponse> listDeposits(@RequestParam(required = false) DepositStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer limit) {
        return adminFinanceService.listDeposits(status, userId, limit);
    }

    @GetMapping("/api/admin/deposit-requests/search")
    public List<DepositResponse> searchDeposits(@RequestParam(required = false) String query,
            @RequestParam(required = false) DepositStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer limit) {
        return adminFinanceService.searchDeposits(query, status, userId, limit);
    }

    @GetMapping("/api/admin/deposit-requests/{depositCode}")
    public DepositResponse getDeposit(@PathVariable String depositCode) {
        return adminFinanceService.getDepositForAdmin(depositCode);
    }

    @GetMapping("/api/admin/deposit-requests/expired")
    public List<DepositResponse> listExpiredDeposits(@RequestParam(required = false) Integer limit) {
        return adminFinanceService.listDeposits(DepositStatus.EXPIRED, null, limit);
    }

    @GetMapping("/api/admin/deposit-requests/manual-review")
    public List<DepositResponse> listManualReviewDeposits(@RequestParam(required = false) Integer limit) {
        return adminFinanceService.listDeposits(DepositStatus.MANUAL_REVIEW, null, limit);
    }

    @PostMapping("/api/admin/deposit-requests/{depositCode}/cancel")
    public DepositResponse cancelDeposit(Authentication authentication, @PathVariable String depositCode,
            @Valid @RequestBody AdminDepositCancelRequest request) {
        return adminFinanceService.cancelDeposit(CurrentUser.require(authentication).userId(), depositCode, request);
    }

    @PostMapping("/api/admin/deposit-requests/{depositCode}/extend")
    public DepositResponse extendDeposit(Authentication authentication, @PathVariable String depositCode,
            @Valid @RequestBody AdminDepositExtendRequest request) {
        return adminFinanceService.extendDeposit(CurrentUser.require(authentication).userId(), depositCode, request);
    }

    @PostMapping("/api/admin/deposit-requests/{depositCode}/manual-credit")
    public DepositResponse manualCreditDeposit(Authentication authentication, @PathVariable String depositCode,
            @Valid @RequestBody ManualCreditDepositRequest request) {
        return adminFinanceService.manualCreditDeposit(CurrentUser.require(authentication).userId(), depositCode, request);
    }

    @GetMapping("/api/admin/bank-transactions")
    public List<AdminBankTransactionResponse> listBankTransactions(
            @RequestParam(required = false) BankTransactionStatus status,
            @RequestParam(required = false) Integer limit) {
        return adminFinanceService.listBankTransactions(status, limit);
    }

    @GetMapping("/api/admin/bank-transactions/search")
    public List<AdminBankTransactionResponse> searchBankTransactions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BankTransactionStatus status,
            @RequestParam(required = false) Integer limit) {
        return adminFinanceService.searchBankTransactions(query, status, limit);
    }

    @GetMapping("/api/admin/bank-transactions/{id}")
    public AdminBankTransactionResponse getBankTransaction(@PathVariable Long id) {
        return adminFinanceService.getBankTransaction(id);
    }

    @GetMapping("/api/admin/bank-transactions/manual-review")
    public List<AdminBankTransactionResponse> listManualReviewBankTransactions(
            @RequestParam(required = false) Integer limit) {
        return adminFinanceService.listBankTransactions(BankTransactionStatus.MANUAL_REVIEW, limit);
    }

    @GetMapping("/api/admin/bank-transactions/duplicate")
    public List<AdminBankTransactionResponse> listDuplicateBankTransactions(
            @RequestParam(required = false) Integer limit) {
        return adminFinanceService.listBankTransactions(BankTransactionStatus.DUPLICATE, limit);
    }

    @GetMapping("/api/admin/bank-transactions/ignored")
    public List<AdminBankTransactionResponse> listIgnoredBankTransactions(
            @RequestParam(required = false) Integer limit) {
        return adminFinanceService.listBankTransactions(BankTransactionStatus.IGNORED, limit);
    }

    @PostMapping("/api/admin/bank-transactions/{id}/manual-credit")
    public AdminBankTransactionResponse manualCreditBankTransaction(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody ManualCreditBankTransactionRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return adminFinanceService.manualCreditBankTransaction(adminUserId, id, request);
    }

    @PostMapping("/api/admin/bank-transactions/bulk-manual-credit")
    public List<AdminBankTransactionResponse> bulkManualCreditBankTransactions(Authentication authentication,
            @Valid @RequestBody BulkManualCreditBankTransactionsRequest request) {
        return adminFinanceService.bulkManualCreditBankTransactions(CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/admin/bank-transactions/{id}/match")
    public AdminBankTransactionResponse matchBankTransaction(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody MatchBankTransactionRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return adminFinanceService.matchBankTransaction(adminUserId, id, request);
    }

    @PostMapping("/api/admin/bank-transactions/{id}/reprocess")
    public AdminBankTransactionResponse reprocessBankTransaction(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody ReprocessBankTransactionRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return adminFinanceService.reprocessBankTransaction(adminUserId, id, request);
    }

    @PostMapping("/api/admin/bank-transactions/{id}/ignore")
    public AdminBankTransactionResponse ignoreBankTransaction(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody IgnoreBankTransactionRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return adminFinanceService.ignoreBankTransaction(adminUserId, id, request);
    }

    @GetMapping("/api/admin/wallet-transactions")
    public List<WalletTransactionResponse> listWalletTransactions(@RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer limit) {
        return adminFinanceService.listWalletTransactions(userId, limit);
    }

    @GetMapping("/api/admin/wallet-transactions/search")
    public List<WalletTransactionResponse> searchWalletTransactions(@RequestParam(required = false) String query,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) WalletTransactionType type,
            @RequestParam(required = false) WalletTransactionDirection direction,
            @RequestParam(required = false) Integer limit) {
        return adminFinanceService.searchWalletTransactions(query, userId, type, direction, limit);
    }

    @GetMapping("/api/admin/wallet-transactions/{id}/details")
    public WalletTransactionResponse getWalletTransaction(@PathVariable Long id) {
        return adminFinanceService.getWalletTransaction(id);
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

    @GetMapping("/api/admin/reports/revenue")
    public com.example.KendyDigital.dto.RevenueReportResponse getRevenueReport() {
        return adminFinanceService.getRevenueReport();
    }
}
