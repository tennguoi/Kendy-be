package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.inventory.request.BulkAccountCredentialImportRequest;
import com.example.KendyDigital.dto.inventory.request.CreateAccountCredentialRequest;
import com.example.KendyDigital.dto.inventory.request.UpdateAccountCredentialRequest;
import com.example.KendyDigital.dto.inventory.response.AccountCredentialAdminResponse;
import com.example.KendyDigital.dto.inventory.response.AccountCredentialRevealResponse;
import com.example.KendyDigital.dto.inventory.response.BulkAccountCredentialImportResponse;
import com.example.KendyDigital.dto.inventory.response.InventoryAlertSummaryResponse;
import com.example.KendyDigital.dto.inventory.response.UserAccountCredentialResponse;
import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.inventory.AccountInventoryService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountInventoryController {
    private final AccountInventoryService accountInventoryService;

    public AccountInventoryController(AccountInventoryService accountInventoryService) {
        this.accountInventoryService = accountInventoryService;
    }

    @GetMapping("/api/me/credentials")
    public List<UserAccountCredentialResponse> listForCurrentUser(Authentication authentication,
            @RequestParam(required = false) Integer limit) {
        return accountInventoryService.listForUser(CurrentUser.require(authentication).userId(), limit);
    }

    @GetMapping("/api/admin/services/{serviceId}/credentials")
    public List<AccountCredentialAdminResponse> listByService(@PathVariable Long serviceId,
            @RequestParam(required = false) AccountCredentialStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(required = false) Instant deliveredFrom,
            @RequestParam(required = false) Instant deliveredTo,
            @RequestParam(required = false) Instant expiresBefore,
            @RequestParam(required = false) Integer limit) {
        if (query != null || createdFrom != null || createdTo != null || deliveredFrom != null
                || deliveredTo != null || expiresBefore != null) {
            return accountInventoryService.searchByService(serviceId, status, query, createdFrom, createdTo,
                    deliveredFrom, deliveredTo, expiresBefore, limit);
        }
        return accountInventoryService.listByService(serviceId, status, limit);
    }

    @GetMapping("/api/admin/assigned-credentials")
    public List<AccountCredentialAdminResponse> listAssigned(
            @RequestParam(required = false) AccountCredentialStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Instant deliveredFrom,
            @RequestParam(required = false) Instant deliveredTo,
            @RequestParam(required = false) Instant expiresBefore,
            @RequestParam(required = false) Integer limit) {
        return accountInventoryService.searchAssigned(status, query, deliveredFrom, deliveredTo, expiresBefore, limit);
    }

    @PostMapping("/api/admin/services/{serviceId}/credentials")
    public AccountCredentialAdminResponse create(Authentication authentication, @PathVariable Long serviceId,
            @Valid @RequestBody CreateAccountCredentialRequest request) {
        return accountInventoryService.create(CurrentUser.require(authentication).userId(), serviceId, request);
    }

    @PostMapping("/api/admin/services/{serviceId}/credentials/bulk-import")
    public BulkAccountCredentialImportResponse bulkImport(Authentication authentication, @PathVariable Long serviceId,
            @Valid @RequestBody BulkAccountCredentialImportRequest request) {
        return accountInventoryService.bulkImport(CurrentUser.require(authentication).userId(), serviceId, request);
    }

    @PutMapping("/api/admin/credentials/{credentialId}")
    public AccountCredentialAdminResponse update(Authentication authentication, @PathVariable Long credentialId,
            @Valid @RequestBody UpdateAccountCredentialRequest request) {
        return accountInventoryService.update(CurrentUser.require(authentication).userId(), credentialId, request);
    }

    @DeleteMapping("/api/admin/credentials/{credentialId}")
    public AccountCredentialAdminResponse disable(Authentication authentication, @PathVariable Long credentialId) {
        return accountInventoryService.disable(CurrentUser.require(authentication).userId(), credentialId);
    }

    @PostMapping("/api/admin/credentials/{credentialId}/reveal")
    public AccountCredentialRevealResponse reveal(Authentication authentication, @PathVariable Long credentialId) {
        var admin = CurrentUser.require(authentication);
        return accountInventoryService.reveal(admin.userId(), admin.role(), credentialId);
    }

    @GetMapping("/api/admin/credentials/alerts")
    public InventoryAlertSummaryResponse alerts(@RequestParam(required = false) Integer lowStockThreshold,
            @RequestParam(required = false) Integer expiringDays) {
        return accountInventoryService.alertSummary(lowStockThreshold, expiringDays);
    }
}
