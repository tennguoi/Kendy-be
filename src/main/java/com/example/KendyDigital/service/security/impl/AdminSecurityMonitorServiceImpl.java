package com.example.KendyDigital.service.security.impl;

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
import com.example.KendyDigital.model.auth.AuthSession;
import com.example.KendyDigital.model.security.AlertStatus;
import com.example.KendyDigital.model.security.AlertSubjectType;
import com.example.KendyDigital.model.security.IpBan;
import com.example.KendyDigital.model.security.IpBanSource;
import com.example.KendyDigital.model.security.SecurityAlert;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserApiKey;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.repository.AuditLogRepository;
import com.example.KendyDigital.repository.AuthSessionRepository;
import com.example.KendyDigital.repository.SecurityAlertRepository;
import com.example.KendyDigital.repository.SecurityEventRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserApiKeyRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import com.example.KendyDigital.service.security.AdminSecurityMonitorService;
import com.example.KendyDigital.service.security.monitor.AlertService;
import com.example.KendyDigital.service.security.monitor.AuditIntegrityService;
import com.example.KendyDigital.service.security.monitor.IpBanService;
import com.example.KendyDigital.service.security.monitor.RiskScoringService;
import com.example.KendyDigital.service.security.monitor.WalletFreezeService;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminSecurityMonitorServiceImpl implements AdminSecurityMonitorService {
    private static final List<UserRole> ADMIN_ROLES = List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN);

    private static final Set<String> CREDENTIAL_ACTIONS = Set.of(
            "ACCOUNT_CREDENTIAL_REVEALED",
            "ACCOUNT_CREDENTIAL_UPDATED",
            "ACCOUNT_CREDENTIAL_DISABLED",
            "ACCOUNT_CREDENTIAL_BULK_IMPORTED",
            "ACCOUNT_CREDENTIAL_CREATED");
    private static final Set<String> WEBHOOK_ACTIONS = Set.of("SEPAY_WEBHOOK_REJECTED");
    private static final Set<String> AUTH_ACTIONS = Set.of(
            "USER_2FA_DISABLED",
            "USER_2FA_RESET",
            "USER_2FA_EMAIL_CODE_LOCKED",
            "USER_PASSWORD_RESET_COMPLETED",
            "USER_LOGIN_UNLOCKED",
            "AUTH_SESSION_REVOKED",
            "USER_API_KEY_REVOKED",
            "SECURITY_SESSION_REVOKED",
            "SECURITY_API_KEY_REVOKED",
            "RATE_LIMIT_EXCEEDED");
    private static final Set<String> ADMIN_ACTIONS = Set.of(
            "ADMIN_2FA_DISABLED",
            "ADMIN_2FA_RESET",
            "ADMIN_ROLE_UPDATED",
            "ADMIN_PERMISSIONS_UPDATED",
            "USER_BULK_STATUS_UPDATED");

    /** Category -> audit actions (O(1) lookup for the events feed). */
    private static final Map<String, Set<String>> CATEGORY_ACTIONS = Map.of(
            "CREDENTIAL", CREDENTIAL_ACTIONS,
            "WEBHOOK", WEBHOOK_ACTIONS,
            "AUTH", AUTH_ACTIONS,
            "ADMIN", ADMIN_ACTIONS);

    private static final Set<String> SENSITIVE_CHANGE_ACTIONS;

    static {
        Set<String> merged = new LinkedHashSet<>(AUTH_ACTIONS);
        merged.addAll(ADMIN_ACTIONS);
        SENSITIVE_CHANGE_ACTIONS = Set.copyOf(merged);
    }

    private final UserAccountRepository userAccountRepository;
    private final AuthSessionRepository authSessionRepository;
    private final UserApiKeyRepository userApiKeyRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final UserNotificationService userNotificationService;
    private final SecurityEventRepository securityEventRepository;
    private final SecurityAlertRepository securityAlertRepository;
    private final AlertService alertService;
    private final IpBanService ipBanService;
    private final RiskScoringService riskScoringService;
    private final AuditIntegrityService auditIntegrityService;
    private final WalletFreezeService walletFreezeService;

    public AdminSecurityMonitorServiceImpl(UserAccountRepository userAccountRepository,
            AuthSessionRepository authSessionRepository,
            UserApiKeyRepository userApiKeyRepository,
            AuditLogRepository auditLogRepository,
            AuditService auditService,
            UserNotificationService userNotificationService,
            SecurityEventRepository securityEventRepository,
            SecurityAlertRepository securityAlertRepository,
            AlertService alertService,
            IpBanService ipBanService,
            RiskScoringService riskScoringService,
            AuditIntegrityService auditIntegrityService,
            WalletFreezeService walletFreezeService) {
        this.userAccountRepository = userAccountRepository;
        this.authSessionRepository = authSessionRepository;
        this.userApiKeyRepository = userApiKeyRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.userNotificationService = userNotificationService;
        this.securityEventRepository = securityEventRepository;
        this.securityAlertRepository = securityAlertRepository;
        this.alertService = alertService;
        this.ipBanService = ipBanService;
        this.riskScoringService = riskScoringService;
        this.auditIntegrityService = auditIntegrityService;
        this.walletFreezeService = walletFreezeService;
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityOverviewResponse overview() {
        Instant now = Instant.now();
        Instant since = now.minus(Duration.ofHours(24));
        return new SecurityOverviewResponse(
                userAccountRepository.countByLockedUntilGreaterThan(now),
                userAccountRepository.countByFailedLoginAttemptsGreaterThan(0),
                authSessionRepository.countByRevokedAtIsNullAndExpiresAtAfter(now),
                userApiKeyRepository.countByRevokedAtIsNull(),
                userAccountRepository.countByRoleInAndTwoFactorEnabledFalse(ADMIN_ROLES),
                userAccountRepository.countByRoleIn(ADMIN_ROLES),
                auditLogRepository.countByActionInAndCreatedAtGreaterThanEqual(WEBHOOK_ACTIONS, since),
                auditLogRepository.countByActionInAndCreatedAtGreaterThanEqual(
                        Set.of("ACCOUNT_CREDENTIAL_REVEALED"), since),
                auditLogRepository.countByActionInAndCreatedAtGreaterThanEqual(SENSITIVE_CHANGE_ACTIONS, since),
                now);
    }

    @Override
    @Transactional(readOnly = true)
    public ThreatOverviewResponse threatOverview() {
        Instant now = Instant.now();
        Instant since = now.minus(Duration.ofHours(24));
        long openAlerts = securityAlertRepository.countByStatus(AlertStatus.OPEN);
        long critical = securityAlertRepository.countByStatusAndSeverity(AlertStatus.OPEN, SecuritySeverity.CRITICAL);
        long high = securityAlertRepository.countByStatusAndSeverity(AlertStatus.OPEN, SecuritySeverity.HIGH);
        long banned = ipBanService.active(500).size();
        long events24h = securityEventRepository.countByOccurredAtGreaterThanEqual(since);
        long loginFailures = securityEventRepository.countByTypeAndOccurredAtGreaterThanEqual(
                SecurityEventType.LOGIN_FAILED, since);
        long rateLimited = securityEventRepository.countByTypeAndOccurredAtGreaterThanEqual(
                SecurityEventType.RATE_LIMITED, since);
        long waf = securityEventRepository.countByTypeAndOccurredAtGreaterThanEqual(SecurityEventType.WAF_SQLI, since)
                + securityEventRepository.countByTypeAndOccurredAtGreaterThanEqual(SecurityEventType.WAF_XSS, since)
                + securityEventRepository.countByTypeAndOccurredAtGreaterThanEqual(SecurityEventType.WAF_TRAVERSAL, since)
                + securityEventRepository.countByTypeAndOccurredAtGreaterThanEqual(SecurityEventType.WAF_SCANNER, since)
                + securityEventRepository.countByTypeAndOccurredAtGreaterThanEqual(SecurityEventType.WAF_TOOL_USER_AGENT, since);
        long webhook = securityEventRepository.countByTypeAndOccurredAtGreaterThanEqual(
                SecurityEventType.WEBHOOK_REJECTED, since);

        int healthScore = computeHealthScore(critical, high, banned, waf);
        List<SecurityEventRepository.TypeAggregate> typeCounts = securityEventRepository.countByTypeSince(since);
        String topAttack = typeCounts.isEmpty() || typeCounts.get(0).getType() == null
                ? null : typeCounts.get(0).getType().name();
        List<SecurityAlertResponse> latestAlerts = alertService.recent(10).stream()
                .map(SecurityAlertResponse::from)
                .toList();
        List<SecurityTopIpResponse> topIps = topIps(24, 10);

        return new ThreatOverviewResponse(healthScore, openAlerts, critical, high, banned, events24h,
                loginFailures, rateLimited, waf, webhook, topAttack, latestAlerts, topIps, now);
    }

    private int computeHealthScore(long critical, long high, long banned, long waf) {
        int score = 100;
        score -= (int) Math.min(50, critical * 10);
        score -= (int) Math.min(30, high * 5);
        score -= (int) Math.min(10, banned);
        score -= (int) Math.min(10, waf / 10);
        return Math.max(0, score);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityRiskyAccountResponse> riskyAccounts(Integer limit) {
        return userAccountRepository.findRiskyAccounts(Instant.now(), PageRequest.of(0, clamp(limit, 50, 200)))
                .stream()
                .map(SecurityRiskyAccountResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SecurityRiskyAccountResponse unlockAccount(Long adminUserId, Long userId) {
        UserAccount user = userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.resetFailedLoginAttempts();
        auditService.recordAdmin(adminUserId, "USER_LOGIN_UNLOCKED", "USER", user.getId(), null);
        return SecurityRiskyAccountResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecuritySessionResponse> activeSessions(int page, int size) {
        return authSessionRepository.findActiveSessions(Instant.now(),
                        PageRequest.of(Math.max(0, page), clamp(size, 50, 200)))
                .stream()
                .map(SecuritySessionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void revokeSession(Long adminUserId, Long sessionId) {
        AuthSession session = authSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        session.revoke();
        auditService.recordAdmin(adminUserId, "SECURITY_SESSION_REVOKED", "AUTH_SESSION", session.getId(),
                "userId=" + session.getUser().getId());
    }

    @Override
    @Transactional
    public void revokeAllSessions(Long adminUserId, Long userId) {
        List<AuthSession> sessions = authSessionRepository.findAllByUser_IdAndRevokedAtIsNull(userId);
        sessions.forEach(AuthSession::revoke);
        authSessionRepository.saveAll(sessions);
        auditService.recordAdmin(adminUserId, "SECURITY_SESSION_REVOKED", "USER", userId,
                "scope=all,count=" + sessions.size());
        userNotificationService.create(userId, "All sessions signed out",
                "An administrator signed out all of your active sessions.", "SECURITY", "/account/security");
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityApiKeyResponse> activeApiKeys(int page, int size) {
        return userApiKeyRepository.findActiveKeys(PageRequest.of(Math.max(0, page), clamp(size, 50, 200)))
                .stream()
                .map(SecurityApiKeyResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void revokeApiKey(Long adminUserId, Long keyId) {
        UserApiKey key = userApiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
        key.revoke();
        Long userId = key.getUser().getId();
        auditService.recordAdmin(adminUserId, "SECURITY_API_KEY_REVOKED", "USER_API_KEY", key.getId(),
                "userId=" + userId + ",keyName=" + key.getName() + ",keyPrefix=" + key.getKeyPrefix());
        userNotificationService.create(userId, "API key revoked by admin",
                "API key '" + key.getName() + "' was revoked by an administrator.", "SECURITY",
                "/account/security");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> events(String category, Integer limit) {
        Set<String> actions;
        if (category == null || category.isBlank()) {
            Set<String> all = new LinkedHashSet<>();
            CATEGORY_ACTIONS.values().forEach(all::addAll);
            actions = all;
        } else {
            actions = CATEGORY_ACTIONS.get(category.trim().toUpperCase(Locale.ROOT));
            if (actions == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown security event category");
            }
        }
        return auditLogRepository
                .findAllByActionInOrderByCreatedAtDesc(actions, PageRequest.of(0, clamp(limit, 50, 200)))
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityAlertResponse> alerts(String status, String severity, Integer limit) {
        AlertStatus parsedStatus = parseEnum(status, AlertStatus.class);
        SecuritySeverity parsedSeverity = parseEnum(severity, SecuritySeverity.class);
        return alertService.list(parsedStatus, parsedSeverity, clamp(limit, 50, 200)).stream()
                .map(SecurityAlertResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SecurityAlertResponse updateAlert(Long adminUserId, Long id, UpdateAlertRequest request) {
        AlertStatus status = parseEnum(request.status(), AlertStatus.class);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown alert status");
        }
        SecurityAlert alert = alertService.updateStatus(id, status, request.assignee(), request.note());
        auditService.recordAdmin(adminUserId, "SECURITY_ALERT_UPDATED", "SECURITY_ALERT", id,
                "status=" + status + ",rule=" + alert.getRuleCode());
        return SecurityAlertResponse.from(alert);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityEventResponse> securityEvents(String type, String severity, String ip, Long userId,
            Instant from, Instant to, Integer limit) {
        SecurityEventType parsedType = parseEnum(type, SecurityEventType.class);
        SecuritySeverity parsedSeverity = parseEnum(severity, SecuritySeverity.class);
        return securityEventRepository.searchSecurityEvents(
                        parsedType == null ? null : parsedType.name(),
                        parsedSeverity == null ? null : parsedSeverity.name(),
                        ip == null || ip.isBlank() ? null : ip.trim(), userId, from, to,
                        PageRequest.of(0, clamp(limit, 100, 500)))
                .stream()
                .map(SecurityEventResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityTopIpResponse> topIps(int hours, Integer limit) {
        Instant from = Instant.now().minus(Duration.ofHours(Math.max(1, Math.min(hours, 168))));
        return securityEventRepository.findTopIps(from, PageRequest.of(0, clamp(limit, 10, 100)))
                .stream()
                .map(aggregate -> new SecurityTopIpResponse(
                        aggregate.getIp(),
                        aggregate.getTotal(),
                        aggregate.getMaxSeverity() == null ? null : aggregate.getMaxSeverity().name(),
                        Math.round(riskScoringService.ipRisk(aggregate.getIp())),
                        null,
                        null,
                        ipBanService.isBanned(aggregate.getIp())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityTimelinePointResponse> timeline(int hours, int bucketMinutes) {
        int safeHours = Math.max(1, Math.min(hours, 168));
        int safeBucket = Math.max(5, Math.min(bucketMinutes, 1440));
        Instant from = Instant.now().minus(Duration.ofHours(safeHours));
        long bucketMillis = safeBucket * 60_000L;

        java.util.Map<Long, long[]> buckets = new java.util.TreeMap<>();
        for (SecurityEventRepository.TimelineRow row : securityEventRepository.findTimelineRowsSince(
                from, PageRequest.of(0, 50_000))) {
            if (row.getOccurredAt() == null) {
                continue;
            }
            long bucketStart = row.getOccurredAt().toEpochMilli() / bucketMillis * bucketMillis;
            long[] counts = buckets.computeIfAbsent(bucketStart, ignored -> new long[5]);
            counts[4]++;
            if (row.getType() == null) {
                continue;
            }
            switch (row.getType()) {
                case LOGIN_FAILED, LOGIN_UNKNOWN_EMAIL -> counts[0]++;
                case RATE_LIMITED -> counts[1]++;
                case WAF_SQLI, WAF_XSS, WAF_TRAVERSAL, WAF_SCANNER, WAF_TOOL_USER_AGENT -> counts[2]++;
                case WEBHOOK_REJECTED -> counts[3]++;
                default -> {
                }
            }
        }
        return buckets.entrySet().stream()
                .map(entry -> new SecurityTimelinePointResponse(
                        Instant.ofEpochMilli(entry.getKey()),
                        entry.getValue()[0], entry.getValue()[1], entry.getValue()[2], entry.getValue()[3],
                        entry.getValue()[4]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityIpProfileResponse ipProfile(String ip, Integer limit) {
        if (ip == null || ip.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IP is required");
        }
        String normalized = ip.trim();
        List<SecurityEventResponse> recent = securityEventRepository.searchSecurityEvents(
                        null, null, normalized, null, null, null, PageRequest.of(0, clamp(limit, 50, 200)))
                .stream()
                .map(SecurityEventResponse::from)
                .toList();
        Instant firstSeen = recent.isEmpty() ? null : recent.get(recent.size() - 1).occurredAt();
        Instant lastSeen = recent.isEmpty() ? null : recent.get(0).occurredAt();
        return new SecurityIpProfileResponse(
                normalized,
                Math.round(riskScoringService.ipRisk(normalized)),
                ipBanService.isBanned(normalized),
                ipBanService.isAllowlisted(normalized),
                firstSeen,
                lastSeen,
                recent.size(),
                recent);
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityUserRiskResponse userRisk(Long userId, Integer limit) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Instant since = Instant.now().minus(Duration.ofHours(24));
        List<SecurityEventResponse> recent = securityEventRepository.searchSecurityEvents(
                        null, null, null, userId, null, null, PageRequest.of(0, clamp(limit, 50, 200)))
                .stream()
                .map(SecurityEventResponse::from)
                .toList();
        long events24h = recent.stream().filter(event -> event.occurredAt().isAfter(since)).count();
        return new SecurityUserRiskResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole() == null ? null : user.getRole().name(),
                Math.round(riskScoringService.userRisk(userId)),
                user.getFailedLoginAttempts(),
                user.isLocked(),
                user.isTwoFactorEnabled(),
                user.isWalletFrozen(),
                user.getLastLoginAt(),
                user.getLastLoginIp(),
                user.getLastLoginCountry(),
                events24h,
                recent);
    }

    @Override
    @Transactional
    public SecurityUserRiskResponse freezeWallet(Long adminUserId, Long userId, FreezeWalletRequest request) {
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "Manual freeze by admin" : request.reason();
        walletFreezeService.freeze(userId, reason, adminUserId);
        alertService.raise("AUTH-13", SecuritySeverity.HIGH, "Wallet frozen", AlertSubjectType.USER,
                String.valueOf(userId), "reason=" + reason + ";actorAdminUserId=" + adminUserId);
        return userRisk(userId, null);
    }

    @Override
    @Transactional
    public SecurityUserRiskResponse unfreezeWallet(Long adminUserId, Long userId, FreezeWalletRequest request) {
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "Manual unfreeze by admin" : request.reason();
        walletFreezeService.unfreeze(userId, reason, adminUserId);
        return userRisk(userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityIpBanResponse> ipBans(Integer limit) {
        return ipBanService.active(clamp(limit, 100, 200)).stream()
                .map(SecurityIpBanResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SecurityIpBanResponse banIp(Long adminUserId, BanIpRequest request) {
        Duration ttl = request.durationMinutes() == null
                ? Duration.ofHours(24)
                : Duration.ofMinutes(Math.max(1, request.durationMinutes()));
        String reason = request.reason() == null || request.reason().isBlank() ? "Manual ban" : request.reason();
        IpBan ban = ipBanService.ban(request.ipOrCidr().trim(), reason, IpBanSource.MANUAL, "MANUAL", adminUserId, ttl);
        if (ban == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not ban the supplied IP/CIDR");
        }
        auditService.recordAdmin(adminUserId, "SECURITY_IP_BANNED", "IP", ban.getId(),
                "ip=" + ban.getIpOrCidr() + ",reason=" + reason);
        alertService.raise("MANUAL-BAN", SecuritySeverity.MEDIUM, "IP banned manually", AlertSubjectType.IP,
                ban.getIpOrCidr(), "reason=" + reason + " by adminUserId=" + adminUserId);
        return SecurityIpBanResponse.from(ban);
    }

    @Override
    @Transactional
    public void unbanIp(Long adminUserId, Long id) {
        ipBanService.unban(id, adminUserId);
        auditService.recordAdmin(adminUserId, "SECURITY_IP_UNBANNED", "IP_BAN", id, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditIntegrityService.IntegrityReport auditIntegrity() {
        return auditIntegrityService.verify();
    }

    private int clamp(Integer value, int fallback, int max) {
        return value == null ? fallback : Math.max(1, Math.min(value, max));
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
