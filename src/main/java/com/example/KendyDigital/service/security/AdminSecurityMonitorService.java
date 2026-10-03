package com.example.KendyDigital.service.security;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityApiKeyResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityOverviewResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityRiskyAccountResponse;
import com.example.KendyDigital.dto.monitoring.response.SecuritySessionResponse;
import java.util.List;

public interface AdminSecurityMonitorService {
    SecurityOverviewResponse overview();

    List<SecurityRiskyAccountResponse> riskyAccounts(Integer limit);

    SecurityRiskyAccountResponse unlockAccount(Long adminUserId, Long userId);

    List<SecuritySessionResponse> activeSessions(int page, int size);

    void revokeSession(Long adminUserId, Long sessionId);

    List<SecurityApiKeyResponse> activeApiKeys(int page, int size);

    void revokeApiKey(Long adminUserId, Long keyId);

    /**
     * @param category one of CREDENTIAL, WEBHOOK, AUTH, ADMIN (case-insensitive); null/blank = all.
     */
    List<AuditLogResponse> events(String category, Integer limit);
}
