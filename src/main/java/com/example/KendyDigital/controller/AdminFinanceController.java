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
import com.example.KendyDigital.dto.AdminUserDetailResponse;
import com.example.KendyDigital.dto.AdminUserResponse;
import com.example.KendyDigital.dto.AdminUserRoleUpdateRequest;
import com.example.KendyDigital.dto.AdminUserStatusUpdateRequest;
import com.example.KendyDigital.dto.AdminWalletAdjustmentRequest;
import com.example.KendyDigital.dto.BalanceIntegrityIssueResponse;
import com.example.KendyDigital.dto.DepositResponse;
import com.example.KendyDigital.dto.IgnoreBankTransactionRequest;
import com.example.KendyDigital.dto.ManualCreditBankTransactionRequest;
import com.example.KendyDigital.dto.WalletTransactionResponse;
import com.example.KendyDigital.model.BankTransactionStatus;
import com.example.KendyDigital.model.DepositStatus;
import com.example.KendyDigital.model.UserStatus;
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
    public List<AdminUserResponse> listUsers(@RequestParam(required = false) UserStatus status) {
        return adminFinanceService.listUsers(status);
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
            @RequestParam(required = false) Long userId) {
        return adminFinanceService.listDeposits(status, userId);
    }

    @GetMapping("/api/admin/bank-transactions")
    public List<AdminBankTransactionResponse> listBankTransactions(
            @RequestParam(required = false) BankTransactionStatus status) {
        return adminFinanceService.listBankTransactions(status);
    }

    @PostMapping("/api/admin/bank-transactions/{id}/manual-credit")
    public AdminBankTransactionResponse manualCreditBankTransaction(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody ManualCreditBankTransactionRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return adminFinanceService.manualCreditBankTransaction(adminUserId, id, request);
    }

    @PostMapping("/api/admin/bank-transactions/{id}/ignore")
    public AdminBankTransactionResponse ignoreBankTransaction(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody IgnoreBankTransactionRequest request) {
        Long adminUserId = CurrentUser.require(authentication).userId();
        return adminFinanceService.ignoreBankTransaction(adminUserId, id, request);
    }

    @GetMapping("/api/admin/wallet-transactions")
    public List<WalletTransactionResponse> listWalletTransactions(@RequestParam(required = false) Long userId) {
        return adminFinanceService.listWalletTransactions(userId);
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
