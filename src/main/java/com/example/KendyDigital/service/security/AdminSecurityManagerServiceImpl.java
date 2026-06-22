package com.example.KendyDigital.service.security;

import com.example.KendyDigital.dto.auth.request.TwoFactorVerifyRequest;
import com.example.KendyDigital.dto.auth.response.AuthSessionResponse;
import com.example.KendyDigital.dto.auth.response.TotpSetupResponse;
import com.example.KendyDigital.dto.role.request.AdminPermissionsRequest;
import com.example.KendyDigital.dto.role.request.AdminRolesRequest;
import com.example.KendyDigital.dto.user.response.AdminUserResponse;
import com.example.KendyDigital.dto.user.response.UserApiKeyResponse;
import com.example.KendyDigital.model.auth.AuthSession;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserApiKey;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.repository.AuthSessionRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserApiKeyRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminSecurityManagerServiceImpl  implements AdminSecurityManagerService{
    private final UserAccountRepository userAccountRepository;
    private final AuthSessionRepository authSessionRepository;
    private final UserApiKeyRepository userApiKeyRepository;
    private final AuditService auditService;
    private final TwoFactorService twoFactorService;
    private final UserNotificationService userNotificationService;

    public AdminSecurityManagerServiceImpl(UserAccountRepository userAccountRepository,
            AuthSessionRepository authSessionRepository,
            UserApiKeyRepository userApiKeyRepository,
            AuditService auditService,
            TwoFactorService twoFactorService,
            UserNotificationService userNotificationService) {
        this.userAccountRepository = userAccountRepository;
        this.authSessionRepository = authSessionRepository;
        this.userApiKeyRepository = userApiKeyRepository;
        this.auditService = auditService;
        this.twoFactorService = twoFactorService;
        this.userNotificationService = userNotificationService;
    }

    @Transactional(readOnly = true)
    public List<AuthSessionResponse> listSessions(Long userId, int page, int size) {
        return authSessionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(AuthSessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuthSessionResponse> listSessions(Long userId, Integer limit) {
        return listSessions(userId, 0, limit == null ? 100 : limit);
    }

    @Transactional
    public void revokeSession(Long adminUserId, Long userId, Long sessionId) {
        AuthSession session = authSessionRepository.findByIdAndUser_Id(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        session.revoke();
        auditService.recordAdmin(adminUserId, "AUTH_SESSION_REVOKED", "AUTH_SESSION", session.getId(),
                "userId=" + userId);
    }

    @Transactional
    public List<AdminUserResponse> bulkUserStatus(Long adminUserId, List<Long> ids, UserStatus status, String reason) {
        List<UserAccount> users = userAccountRepository.findAllByIdForUpdate(ids);
        if (users.size() != ids.size()) {
            List<Long> found = users.stream().map(UserAccount::getId).toList();
            Long missing = ids.stream().filter(id -> !found.contains(id)).findFirst().orElse(null);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User not found: " + missing);
        }
        return users.stream()
                .map(user -> {
                    user.setStatus(status);
                    auditService.recordAdmin(adminUserId, "USER_BULK_STATUS_UPDATED", "USER", user.getId(),
                            "status=" + status + ",reason=" + blankToNull(reason));
                    return AdminUserResponse.from(user);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listAdmins(Integer limit) {
        return userAccountRepository.findAllByRoleInOrderByCreatedAtDesc(
                        List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN), page(limit))
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public TotpSetupResponse setupTwoFactor(Long adminUserId, Long targetAdminId) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        if (admin.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA is already enabled");
        }
        String secret = twoFactorService.generateSecret();
        List<String> backupCodes = twoFactorService.generateBackupCodes();
        String qrBase64 = twoFactorService.qrCodeBase64(secret, admin.getEmail(), "KendyDigital");
        admin.setTwoFactorSecret(secret);
        admin.setBackupCodes(twoFactorService.hashStoredBackupCodes(backupCodes));
        auditService.recordAdmin(adminUserId, "ADMIN_2FA_SETUP", "USER", admin.getId(), null);
        return new TotpSetupResponse(secret, qrBase64, backupCodes);
    }

    @Transactional
    public AdminUserResponse verifyAndEnableTwoFactor(Long adminUserId, Long targetAdminId,
            TwoFactorVerifyRequest request) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        if (admin.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA is already enabled");
        }
        if (admin.getTwoFactorSecret() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "2FA not initialized. Call setup first.");
        }
        if (twoFactorService.verify(admin.getTwoFactorSecret(), request.code())) {
            admin.enableTwoFactor(admin.getTwoFactorSecret(), admin.getBackupCodes());
            auditService.recordAdmin(adminUserId, "ADMIN_2FA_ENABLED", "USER", admin.getId(), null);
            return AdminUserResponse.from(admin);
        }
        if (twoFactorService.verifyBackupCode(admin.getBackupCodes(), request.code())) {
            admin.setBackupCodes(twoFactorService.removeUsedBackupCode(admin.getBackupCodes(), request.code()));
            admin.enableTwoFactor(admin.getTwoFactorSecret(), admin.getBackupCodes());
            auditService.recordAdmin(adminUserId, "ADMIN_2FA_ENABLED_VIA_BACKUP", "USER", admin.getId(), null);
            return AdminUserResponse.from(admin);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
    }

    @Transactional
    public AdminUserResponse disableTwoFactor(Long adminUserId, Long targetAdminId) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        admin.disableTwoFactor();
        auditService.recordAdmin(adminUserId, "ADMIN_2FA_DISABLED", "USER", admin.getId(), null);
        return AdminUserResponse.from(admin);
    }

    @Transactional
    public TotpSetupResponse resetTwoFactor(Long adminUserId, Long targetAdminId) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        admin.resetTwoFactor();
        auditService.recordAdmin(adminUserId, "ADMIN_2FA_RESET", "USER", admin.getId(), null);
        return setupTwoFactor(adminUserId, targetAdminId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPermissions(Long adminId) {
        UserAccount admin = requireAdmin(adminId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("adminId", admin.getId());
        response.put("permissions", parsePermissions(admin.getAdminPermissions()));
        return response;
    }

    @Transactional
    public Map<String, Object> updatePermissions(Long adminUserId, Long targetAdminId,
            AdminPermissionsRequest request) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        String permissions = request.permissions() == null ? "" : request.permissions().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
        admin.setAdminPermissions(permissions);
        auditService.recordAdmin(adminUserId, "ADMIN_PERMISSIONS_UPDATED", "USER", admin.getId(),
                "permissions=" + permissions + ",reason=" + blankToNull(request.reason()));
        return getPermissions(admin.getId());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRoles(Long adminId) {
        UserAccount admin = requireAdmin(adminId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("adminId", admin.getId());
        response.put("role", admin.getRole());
        return response;
    }

    @Transactional
    public Map<String, Object> updateRoles(Long adminUserId, Long targetAdminId, AdminRolesRequest request) {
        UserAccount admin = requireAdminForUpdate(targetAdminId);
        if (request.role() == UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin role cannot be USER");
        }
        admin.setRole(request.role());
        auditService.recordAdmin(adminUserId, "ADMIN_ROLE_UPDATED", "USER", admin.getId(),
                "role=" + request.role() + ",reason=" + blankToNull(request.reason()));
        return getRoles(admin.getId());
    }

    @Transactional(readOnly = true)
    public List<UserApiKeyResponse> listUserApiKeys(Long userId, int page, int size) {
        return userApiKeyRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(UserApiKeyResponse::from)
                .toList();
    }

    @Transactional
    public void revokeUserApiKey(Long adminUserId, Long userId, Long keyId) {
        UserApiKey key = userApiKeyRepository.findByIdAndUser_Id(keyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
        key.revoke();
        auditService.recordAdmin(adminUserId, "USER_API_KEY_REVOKED", "USER_API_KEY", key.getId(),
                "userId=" + userId + ",keyName=" + key.getName() + ",keyPrefix=" + key.getKeyPrefix());
        userNotificationService.create(userId, "API key revoked by admin",
                "API key '" + key.getName() + "' was revoked by an administrator.", "SECURITY", "/account/security");
    }

    private UserAccount requireAdmin(Long adminId) {
        UserAccount admin = userAccountRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (admin.getRole() == UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an admin");
        }
        return admin;
    }

    private UserAccount requireAdminForUpdate(Long adminId) {
        UserAccount admin = userAccountRepository.findByIdForUpdate(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (admin.getRole() == UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not an admin");
        }
        return admin;
    }

    private List<String> parsePermissions(String permissions) {
        if (permissions == null || permissions.isBlank()) {
            return List.of();
        }
        return Arrays.stream(permissions.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        return PageRequest.of(0, normalizedLimit);
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 500)));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
