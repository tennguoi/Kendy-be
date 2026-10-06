package com.example.KendyDigital.service.security;

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
import com.example.KendyDigital.service.security.monitor.AuditIntegrityService;
import java.time.Instant;
import java.util.List;

public interface AdminSecurityMonitorService {
    SecurityOverviewResponse overview();

    ThreatOverviewResponse threatOverview();

    List<SecurityRiskyAccountResponse> riskyAccounts(Integer limit);

    SecurityRiskyAccountResponse unlockAccount(Long adminUserId, Long userId);

    List<SecuritySessionResponse> activeSessions(int page, int size);

    void revokeSession(Long adminUserId, Long sessionId);

    void revokeAllSessions(Long adminUserId, Long userId);

    List<SecurityApiKeyResponse> activeApiKeys(int page, int size);

    void revokeApiKey(Long adminUserId, Long keyId);

    /**
     * @param category one of CREDENTIAL, WEBHOOK, AUTH, ADMIN (case-insensitive); null/blank = all.
     */
    List<AuditLogResponse> events(String category, Integer limit);

    List<SecurityAlertResponse> alerts(String status, String severity, Integer limit);

    SecurityAlertResponse updateAlert(Long adminUserId, Long id, UpdateAlertRequest request);

    List<SecurityEventResponse> securityEvents(String type, String severity, String ip, Long userId, Instant from,
            Instant to, Integer limit);

    List<SecurityTopIpResponse> topIps(int hours, Integer limit);

    List<SecurityTimelinePointResponse> timeline(int hours, int bucketMinutes);

    SecurityIpProfileResponse ipProfile(String ip, Integer limit);

    SecurityUserRiskResponse userRisk(Long userId, Integer limit);

    SecurityUserRiskResponse freezeWallet(Long adminUserId, Long userId, FreezeWalletRequest request);

    SecurityUserRiskResponse unfreezeWallet(Long adminUserId, Long userId, FreezeWalletRequest request);

    List<SecurityIpBanResponse> ipBans(Integer limit);

    SecurityIpBanResponse banIp(Long adminUserId, BanIpRequest request);

    void unbanIp(Long adminUserId, Long id);

    AuditIntegrityService.IntegrityReport auditIntegrity();
}
