package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.dto.monitoring.request.BanIpRequest;
import com.example.KendyDigital.dto.monitoring.request.FreezeWalletRequest;
import com.example.KendyDigital.dto.monitoring.request.UpdateAlertRequest;
import com.example.KendyDigital.dto.monitoring.response.SecurityAlertResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityApiKeyResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityEventResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityIpBanResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityIpProfileResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityOverviewResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityRiskyAccountResponse;
import com.example.KendyDigital.dto.monitoring.response.SecuritySessionResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityTimelinePointResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityTopIpResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityUserRiskResponse;
import com.example.KendyDigital.dto.monitoring.response.ThreatOverviewResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.security.AdminSecurityMonitorService;
import com.example.KendyDigital.service.security.monitor.AuditIntegrityService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read endpoints are open to every admin; mutating actions (unlock / revoke / ban) require
 * SUPER_ADMIN, consistent with {@link AdminSecurityController}.
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

    @GetMapping("/api/admin/security/threat-overview")
    public ThreatOverviewResponse threatOverview() {
        return monitorService.threatOverview();
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

    @PostMapping("/api/admin/security/users/{id}/revoke-all-sessions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void revokeAllSessions(Authentication authentication, @PathVariable Long id) {
        monitorService.revokeAllSessions(CurrentUser.require(authentication).userId(), id);
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

    @GetMapping("/api/admin/security/alerts")
    public List<SecurityAlertResponse> alerts(@RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Integer limit) {
        return monitorService.alerts(status, severity, limit);
    }

    @PatchMapping("/api/admin/security/alerts/{id}")
    public SecurityAlertResponse updateAlert(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody UpdateAlertRequest request) {
        return monitorService.updateAlert(CurrentUser.require(authentication).userId(), id, request);
    }

    @GetMapping("/api/admin/security/security-events")
    public List<SecurityEventResponse> securityEvents(@RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer limit) {
        return monitorService.securityEvents(type, severity, ip, userId, from, to, limit);
    }

    @GetMapping("/api/admin/security/threats/top-ips")
    public List<SecurityTopIpResponse> topIps(@RequestParam(defaultValue = "24") int hours,
            @RequestParam(required = false) Integer limit) {
        return monitorService.topIps(hours, limit);
    }

    @GetMapping("/api/admin/security/timeline")
    public List<SecurityTimelinePointResponse> timeline(@RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "60") int bucket) {
        return monitorService.timeline(hours, bucket);
    }

    @GetMapping("/api/admin/security/ip/{ip}")
    public SecurityIpProfileResponse ipProfile(@PathVariable String ip,
            @RequestParam(required = false) Integer limit) {
        return monitorService.ipProfile(ip, limit);
    }

    @GetMapping("/api/admin/security/users/{id}/risk")
    public SecurityUserRiskResponse userRisk(@PathVariable Long id,
            @RequestParam(required = false) Integer limit) {
        return monitorService.userRisk(id, limit);
    }

    @PostMapping("/api/admin/security/users/{id}/freeze-wallet")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SecurityUserRiskResponse freezeWallet(Authentication authentication, @PathVariable Long id,
            @RequestBody(required = false) FreezeWalletRequest request) {
        return monitorService.freezeWallet(CurrentUser.require(authentication).userId(), id, request);
    }

    @PostMapping("/api/admin/security/users/{id}/unfreeze-wallet")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SecurityUserRiskResponse unfreezeWallet(Authentication authentication, @PathVariable Long id,
            @RequestBody(required = false) FreezeWalletRequest request) {
        return monitorService.unfreezeWallet(CurrentUser.require(authentication).userId(), id, request);
    }

    @GetMapping("/api/admin/security/ip-bans")
    public List<SecurityIpBanResponse> ipBans(@RequestParam(required = false) Integer limit) {
        return monitorService.ipBans(limit);
    }

    @PostMapping("/api/admin/security/ip-bans")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SecurityIpBanResponse banIp(Authentication authentication, @Valid @RequestBody BanIpRequest request) {
        return monitorService.banIp(CurrentUser.require(authentication).userId(), request);
    }

    @DeleteMapping("/api/admin/security/ip-bans/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void unbanIp(Authentication authentication, @PathVariable Long id) {
        monitorService.unbanIp(CurrentUser.require(authentication).userId(), id);
    }

    @GetMapping("/api/admin/security/audit-integrity")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AuditIntegrityService.IntegrityReport auditIntegrity() {
        return monitorService.auditIntegrity();
    }
}
