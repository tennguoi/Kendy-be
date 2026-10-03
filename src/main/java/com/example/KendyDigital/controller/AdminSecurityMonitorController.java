package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityApiKeyResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityOverviewResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityRiskyAccountResponse;
import com.example.KendyDigital.dto.monitoring.response.SecuritySessionResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.security.AdminSecurityMonitorService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read endpoints are open to every admin; mutating actions (unlock / revoke) require SUPER_ADMIN,
 * consistent with {@link AdminSecurityController}.
 */
@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminSecurityMonitorController {
    private final AdminSecurityMonitorService monitorService;

    public AdminSecurityMonitorController(AdminSecurityMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/api/admin/security/overview")
    public SecurityOverviewResponse overview() {
        return monitorService.overview();
    }

    @GetMapping("/api/admin/security/risky-accounts")
    public List<SecurityRiskyAccountResponse> riskyAccounts(@RequestParam(required = false) Integer limit) {
        return monitorService.riskyAccounts(limit);
    }

    @PostMapping("/api/admin/security/users/{id}/unlock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SecurityRiskyAccountResponse unlock(Authentication authentication, @PathVariable Long id) {
        return monitorService.unlockAccount(CurrentUser.require(authentication).userId(), id);
    }

    @GetMapping("/api/admin/security/sessions")
    public List<SecuritySessionResponse> sessions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return monitorService.activeSessions(page, size);
    }

    @DeleteMapping("/api/admin/security/sessions/{sessionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void revokeSession(Authentication authentication, @PathVariable Long sessionId) {
        monitorService.revokeSession(CurrentUser.require(authentication).userId(), sessionId);
    }

    @GetMapping("/api/admin/security/api-keys")
    public List<SecurityApiKeyResponse> apiKeys(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return monitorService.activeApiKeys(page, size);
    }

    @DeleteMapping("/api/admin/security/api-keys/{keyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void revokeApiKey(Authentication authentication, @PathVariable Long keyId) {
        monitorService.revokeApiKey(CurrentUser.require(authentication).userId(), keyId);
    }

    @GetMapping("/api/admin/security/events")
    public List<AuditLogResponse> events(@RequestParam(required = false) String category,
            @RequestParam(required = false) Integer limit) {
        return monitorService.events(category, limit);
    }
}
