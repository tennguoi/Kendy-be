package com.example.KendyDigital.service.security.impl;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityApiKeyResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityOverviewResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityRiskyAccountResponse;
import com.example.KendyDigital.dto.monitoring.response.SecuritySessionResponse;
import com.example.KendyDigital.model.auth.AuthSession;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserApiKey;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.repository.AuditLogRepository;
import com.example.KendyDigital.repository.AuthSessionRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserApiKeyRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import com.example.KendyDigital.service.security.AdminSecurityMonitorService;
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
            "SECURITY_API_KEY_REVOKED");
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

    public AdminSecurityMonitorServiceImpl(UserAccountRepository userAccountRepository,
            AuthSessionRepository authSessionRepository,
            UserApiKeyRepository userApiKeyRepository,
            AuditLogRepository auditLogRepository,
            AuditService auditService,
            UserNotificationService userNotificationService) {
        this.userAccountRepository = userAccountRepository;
        this.authSessionRepository = authSessionRepository;
        this.userApiKeyRepository = userApiKeyRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.userNotificationService = userNotificationService;
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

    private int clamp(Integer value, int fallback, int max) {
        return value == null ? fallback : Math.max(1, Math.min(value, max));
    }
}
