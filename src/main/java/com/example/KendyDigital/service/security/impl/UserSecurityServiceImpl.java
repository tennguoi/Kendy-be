package com.example.KendyDigital.service.security.impl;

import com.example.KendyDigital.service.security.*;

import com.example.KendyDigital.common.ClientIpResolver;
import com.example.KendyDigital.dto.auth.request.AuthEmailRequest;
import com.example.KendyDigital.dto.auth.request.AuthForgotPasswordRequest;
import com.example.KendyDigital.dto.auth.request.AuthResetPasswordRequest;
import com.example.KendyDigital.dto.auth.request.AuthVerifyEmailRequest;
import com.example.KendyDigital.dto.auth.request.AuthVerifyPasswordResetRequest;
import com.example.KendyDigital.dto.auth.request.TwoFactorDisableRequest;
import com.example.KendyDigital.dto.auth.request.TwoFactorVerifyRequest;
import com.example.KendyDigital.dto.auth.response.AuthSessionResponse;
import com.example.KendyDigital.dto.auth.response.AuthTokenResponse;
import com.example.KendyDigital.dto.auth.response.AuthUserResponse;
import com.example.KendyDigital.dto.auth.response.SecurityTokenResponse;
import com.example.KendyDigital.dto.auth.response.TotpSetupResponse;
import com.example.KendyDigital.dto.user.request.UserApiKeyCreateRequest;
import com.example.KendyDigital.dto.user.response.UserApiKeyCreatedResponse;
import com.example.KendyDigital.dto.user.response.UserApiKeyResponse;
import com.example.KendyDigital.dto.user.response.UserSecurityOverviewResponse;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserSecurityToken;
import com.example.KendyDigital.model.user.UserSecurityTokenType;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.security.ResolvedApiKey;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserSecurityTokenRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.auth.AuthTokenService;
import com.example.KendyDigital.service.notification.EmailNotificationService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import com.example.KendyDigital.service.security.apikey.UserApiKeyService;
import com.example.KendyDigital.service.security.monitor.SecuritySignal;
import com.example.KendyDigital.service.security.monitor.SecuritySignalService;
import com.example.KendyDigital.service.security.session.UserSessionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserSecurityServiceImpl  implements UserSecurityService{
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSecurityServiceImpl.class);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);
    private static final Duration EMAIL_VERIFY_TTL = Duration.ofHours(24);
    private static final Duration EMAIL_2FA_TTL = Duration.ofMinutes(10);
    private static final Duration OAUTH_2FA_CHALLENGE_TTL = Duration.ofMinutes(10);
    private static final String EMAIL_TWO_FACTOR_SECRET = "EMAIL_2FA";
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final long BRUTE_FORCE_DELAY_MILLIS = 1000;

    private final UserAccountRepository userAccountRepository;
    private final UserSessionService userSessionService;
    private final UserSecurityTokenRepository securityTokenRepository;
    private final UserApiKeyService userApiKeyService;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final TwoFactorService twoFactorService;
    private final AuditService auditService;
    private final UserNotificationService userNotificationService;
    private final EmailNotificationService emailNotificationService;
    private final SecuritySignalService securitySignalService;
    private final ClientIpResolver clientIpResolver;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserSecurityServiceImpl(UserAccountRepository userAccountRepository,
            UserSessionService userSessionService,
            UserSecurityTokenRepository securityTokenRepository,
            UserApiKeyService userApiKeyService,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService,
            TwoFactorService twoFactorService,
            AuditService auditService,
            UserNotificationService userNotificationService,
            EmailNotificationService emailNotificationService,
            SecuritySignalService securitySignalService,
            ClientIpResolver clientIpResolver) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionService = userSessionService;
        this.securityTokenRepository = securityTokenRepository;
        this.userApiKeyService = userApiKeyService;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.twoFactorService = twoFactorService;
        this.auditService = auditService;
        this.userNotificationService = userNotificationService;
        this.emailNotificationService = emailNotificationService;
        this.securitySignalService = securitySignalService;
        this.clientIpResolver = clientIpResolver;
    }

    @Transactional
    public SecurityTokenResponse forgotPassword(AuthForgotPasswordRequest request) {
        Optional<UserAccount> user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()));
        if (user.isEmpty()) {
            return new SecurityTokenResponse("If the email exists, a reset token has been issued.", null, null);
        }
        String code;
        String codeHash;
        do {
            code = sixDigitCode();
            codeHash = sha256(code);
        } while (securityTokenRepository.existsByTokenHash(codeHash));
        Instant expiresAt = Instant.now().plus(PASSWORD_RESET_TTL);
        securityTokenRepository.save(new UserSecurityToken(
                user.get(),
                UserSecurityTokenType.PASSWORD_RESET,
                codeHash,
                expiresAt));
        auditService.recordSystem("USER_PASSWORD_RESET_REQUESTED", "USER", user.get().getId(), null);
        emailNotificationService.sendPasswordReset(user.get(), code, expiresAt);
        return new SecurityTokenResponse("If the email exists, a reset token has been issued.", null, null);
    }

    @Transactional
    public SecurityTokenResponse verifyPasswordReset(AuthVerifyPasswordResetRequest request) {
        UserSecurityToken token = requireUsableToken(request.token(), UserSecurityTokenType.PASSWORD_RESET);
        UserAccount user = token.getUser();
        token.markUsed();
        IssuedSecurityToken issued = issueSecurityToken(user, UserSecurityTokenType.PASSWORD_RESET_CONFIRMED,
                PASSWORD_RESET_TTL);
        auditService.recordSystem("USER_PASSWORD_RESET_TOKEN_VERIFIED", "USER", user.getId(), null);
        return new SecurityTokenResponse("Password reset token verified.", issued.expiresAt(), issued.token());
    }

    @Transactional
    public AuthUserResponse resetPassword(AuthResetPasswordRequest request) {
        UserSecurityToken token = requireUsableToken(request.token(), UserSecurityTokenType.PASSWORD_RESET_CONFIRMED);
        UserAccount user = token.getUser();
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        token.markUsed();
        revokeAllSessions(user.getId());
        auditService.recordSystem("USER_PASSWORD_RESET_COMPLETED", "USER", user.getId(), null);
        recordAccountChange(SecurityEventType.PASSWORD_RESET, user);
        userNotificationService.create(user.getId(), "Password changed",
                "Your password was reset successfully.", "SECURITY", "/account/security");
        return AuthUserResponse.from(user);
    }

    @Transactional
    public SecurityTokenResponse resendVerification(AuthEmailRequest request) {
        userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .ifPresent(this::sendEmailVerification);
        return new SecurityTokenResponse(
                "If the email exists and is not verified, a verification email has been sent.", null, null);
    }

    @Transactional
    public SecurityTokenResponse sendEmailVerification(UserAccount user) {
        LOGGER.info("=== UserSecurityService.sendEmailVerification CALLED === userId={}, email={}, emailVerifiedAt={}", 
            user.getId(), user.getEmail(), user.getEmailVerifiedAt());
        if (user.getEmailVerifiedAt() != null) {
            LOGGER.warn("Email already verified for userId={}", user.getId());
            return new SecurityTokenResponse("Email already verified.", null, null);
        }
        String code;
        String codeHash;
        do {
            code = sixDigitCode();
            codeHash = sha256(code);
        } while (securityTokenRepository.existsByTokenHash(codeHash));
        Instant expiresAt = Instant.now().plus(EMAIL_VERIFY_TTL);
        securityTokenRepository.save(new UserSecurityToken(
                user,
                UserSecurityTokenType.EMAIL_VERIFICATION,
                codeHash,
                expiresAt));
        auditService.recordSystem("USER_EMAIL_VERIFICATION_REQUESTED", "USER", user.getId(), null);
        LOGGER.info("Verification code generated: code={}, expiresAt={}, token saved to DB", code, expiresAt);
        emailNotificationService.sendEmailVerification(user, code, expiresAt);
        LOGGER.info("=== UserSecurityService.sendEmailVerification COMPLETED === userId={}", user.getId());
        return new SecurityTokenResponse("Email verification email sent.", expiresAt, null);
    }

    @Transactional
    public AuthUserResponse verifyEmail(AuthVerifyEmailRequest request) {
        UserSecurityToken token = requireUsableToken(request.token(), UserSecurityTokenType.EMAIL_VERIFICATION);
        UserAccount user = token.getUser();
        user.verifyEmail();
        token.markUsed();
        auditService.recordSystem("USER_EMAIL_VERIFIED", "USER", user.getId(), null);
        userNotificationService.create(user.getId(), "Email verified",
                "Your email address has been verified.", "SECURITY", "/account/security");
        return AuthUserResponse.from(user);
    }

    @Transactional
    public AuthTokenResponse refresh(String token) {
        UserAccount user = authTokenService.resolveUser(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
        authTokenService.revoke(token, null);
        AuthTokenService.IssuedToken issued = authTokenService.issue(user, null);
        return new AuthTokenResponse(issued.token(), issued.expiresAt(), AuthUserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserSecurityOverviewResponse overview(Long userId) {
        UserAccount user = requireUser(userId);
        return new UserSecurityOverviewResponse(
                user.getId(),
                user.getEmailVerifiedAt() != null,
                user.getEmailVerifiedAt(),
                user.isTwoFactorEnabled(),
                user.getPasswordChangedAt(),
                userSessionService.countActiveSessions(userId),
                userApiKeyService.countActiveKeys(userId),
                user.hasPassword(),
                user.getOauthProvider());
    }

    @Transactional(readOnly = true)
    public List<com.example.KendyDigital.dto.auth.response.AuthSessionResponse> sessions(Long userId, int page, int size) {
        return userSessionService.sessions(userId, page, size);
    }

    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        userSessionService.revokeSession(userId, sessionId);
    }

    @Transactional
    public void revokeAllSessions(Long userId) {
        userSessionService.revokeAllSessions(userId);
    }

    @Transactional
    public TotpSetupResponse setupTwoFactor(Long userId) {
        UserAccount user = requireUserForUpdate(userId);
        if (user.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA is already enabled");
        }
        String secret = twoFactorService.generateSecret();
        List<String> backupCodes = twoFactorService.generateBackupCodes();
        user.setTwoFactorSecret(secret);
        user.setBackupCodes(twoFactorService.hashStoredBackupCodes(backupCodes));
        auditService.recordSystem("USER_2FA_SETUP", "USER", user.getId(), null);
        emailNotificationService.sendSecurityAlert(user, "KendyDigital 2FA setup started",
                "A two-factor authentication setup was started for your account.");
        return new TotpSetupResponse(secret, twoFactorService.qrCodeBase64(secret, user.getEmail(), "KendyDigital"),
                backupCodes);
    }

    @Transactional
    public AuthUserResponse enableTwoFactor(Long userId, TwoFactorVerifyRequest request) {
        UserAccount user = requireUserForUpdate(userId);
        if (user.getTwoFactorSecret() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "2FA not initialized. Call setup first.");
        }
        if (!verifyTwoFactorOrBackup(user, request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }
        user.enableTwoFactor(user.getTwoFactorSecret(), user.getBackupCodes());
        auditService.recordSystem("USER_2FA_ENABLED", "USER", user.getId(), null);
        emailNotificationService.sendSecurityAlert(user, "Two-factor authentication enabled",
                "Two-factor authentication has been enabled on your account.");
        return AuthUserResponse.from(user);
    }

    @Transactional
    public SecurityTokenResponse requestEmailTwoFactorEnable(Long userId) {
        UserAccount user = requireUser(userId);
        if (user.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA is already enabled");
        }
        return sendTwoFactorEmailCode(user);
    }

    @Transactional
    public AuthUserResponse enableEmailTwoFactor(Long userId, TwoFactorVerifyRequest request) {
        UserAccount user = requireUserForUpdate(userId);
        if (user.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA is already enabled");
        }
        if (!verifyEmailTwoFactorCode(user, request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }
        user.enableTwoFactor(EMAIL_TWO_FACTOR_SECRET, null);
        auditService.recordSystem("USER_EMAIL_2FA_ENABLED", "USER", user.getId(), null);
        return AuthUserResponse.from(user);
    }

    @Transactional
    public AuthUserResponse disableTwoFactor(Long userId, TwoFactorDisableRequest request) {
        UserAccount user = requireUserForUpdate(userId);
        requirePassword(user, request.password());
        if (!user.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA is not enabled");
        }
        if (!verifyTwoFactorOrBackup(user, request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }
        user.disableTwoFactor();
        auditService.recordSystem("USER_2FA_DISABLED", "USER", user.getId(), null);
        recordAccountChange(SecurityEventType.TWO_FACTOR_DISABLED, user);
        userNotificationService.create(user.getId(), "Two-factor authentication disabled",
                "2FA was disabled for your account.", "SECURITY", "/account/security");
        return AuthUserResponse.from(user);
    }

    @Transactional
    public TotpSetupResponse resetTwoFactor(Long userId, TwoFactorDisableRequest request) {
        UserAccount user = requireUserForUpdate(userId);
        requirePassword(user, request.password());
        if (user.isTwoFactorEnabled() && !verifyTwoFactorOrBackup(user, request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }
        user.resetTwoFactor();
        auditService.recordSystem("USER_2FA_RESET", "USER", user.getId(), null);
        recordAccountChange(SecurityEventType.TWO_FACTOR_DISABLED, user);
        emailNotificationService.sendSecurityAlert(user, "KendyDigital 2FA reset",
                "Two-factor authentication was reset for your account.");
        return setupTwoFactor(userId);
    }

    @Transactional
    public TotpSetupResponse regenerateBackupCodes(Long userId, TwoFactorDisableRequest request) {
        UserAccount user = requireUserForUpdate(userId);
        requirePassword(user, request.password());
        if (!user.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA is not enabled");
        }
        if (!verifyTwoFactorOrBackup(user, request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }
        List<String> backupCodes = twoFactorService.generateBackupCodes();
        user.setBackupCodes(twoFactorService.hashStoredBackupCodes(backupCodes));
        auditService.recordSystem("USER_2FA_BACKUP_CODES_REGENERATED", "USER", user.getId(), null);
        emailNotificationService.sendSecurityAlert(user, "KendyDigital backup codes regenerated",
                "Your two-factor authentication backup codes were regenerated.");
        return new TotpSetupResponse(user.getTwoFactorSecret(),
                twoFactorService.qrCodeBase64(user.getTwoFactorSecret(), user.getEmail(), "KendyDigital"),
                backupCodes);
    }

    @Transactional(readOnly = true)
    public List<UserApiKeyResponse> apiKeys(Long userId, int page, int size) {
        return userApiKeyService.apiKeys(userId, page, size);
    }

    @Transactional
    public UserApiKeyCreatedResponse createApiKey(Long userId, UserApiKeyCreateRequest request) {
        return userApiKeyService.createApiKey(userId, request);
    }

    @Transactional
    public void revokeApiKey(Long userId, Long keyId) {
        userApiKeyService.revokeApiKey(userId, keyId);
    }

    @Transactional
    public SecurityTokenResponse sendTwoFactorEmailCode(UserAccount user) {
        String code = sixDigitCode();
        Instant expiresAt = Instant.now().plus(EMAIL_2FA_TTL);
        securityTokenRepository.save(new UserSecurityToken(
                user,
                UserSecurityTokenType.EMAIL_2FA,
                sha256(code),
                expiresAt));
        auditService.recordSystem("USER_2FA_EMAIL_CODE_SENT", "USER", user.getId(), null);
        emailNotificationService.sendTwoFactorCode(user, code, expiresAt);
        return new SecurityTokenResponse("2FA email code sent.", expiresAt, null);
    }

    @Transactional
    public SecurityTokenResponse issueOAuthTwoFactorChallenge(UserAccount user) {
        sendTwoFactorEmailCode(user);
        IssuedSecurityToken challenge = issueSecurityToken(user, UserSecurityTokenType.OAUTH_2FA_CHALLENGE,
                OAUTH_2FA_CHALLENGE_TTL);
        return new SecurityTokenResponse("OAuth 2FA challenge issued.", challenge.expiresAt(), challenge.token());
    }

    @Transactional
    public AuthTokenResponse verifyOAuthTwoFactor(String challengeToken, String code) {
        UserSecurityToken challenge = requireUsableToken(challengeToken, UserSecurityTokenType.OAUTH_2FA_CHALLENGE);
        UserAccount user = challenge.getUser();
        if (!verifyEmailTwoFactorCode(user, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }
        challenge.markUsed();
        AuthTokenService.IssuedToken issued = authTokenService.issue(user, null);
        auditService.recordSystem("USER_OAUTH_2FA_VERIFIED", "USER", user.getId(), null);
        return new AuthTokenResponse(issued.token(), issued.expiresAt(), AuthUserResponse.from(user));
    }

    @Transactional
    public boolean verifyEmailTwoFactorCode(UserAccount user, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        List<UserSecurityToken> pendingTokens = securityTokenRepository
                .findByUser_IdAndTypeAndUsedAtIsNullOrderByCreatedAtDesc(
                        user.getId(), UserSecurityTokenType.EMAIL_2FA);

        for (UserSecurityToken pending : pendingTokens) {
            if (pending.getFailedAttempts() >= MAX_OTP_ATTEMPTS) {
                pending.markUsed();
                auditService.recordSystem("USER_2FA_EMAIL_CODE_LOCKED", "USER", user.getId(),
                        "Too many failed attempts: " + pending.getFailedAttempts());
            }
        }

        Optional<UserSecurityToken> token = securityTokenRepository
                .findByUser_IdAndTokenHashAndTypeAndUsedAtIsNull(
                        user.getId(), sha256(code.trim()), UserSecurityTokenType.EMAIL_2FA);

        if (token.isEmpty() || !token.get().isUsable()) {
            pendingTokens.stream().filter(t -> t.getUsedAt() == null).findFirst()
                    .ifPresent(UserSecurityToken::incrementFailedAttempts);
            sleep(BRUTE_FORCE_DELAY_MILLIS);
            return false;
        }

        token.get().markUsed();
        auditService.recordSystem("USER_2FA_EMAIL_CODE_VERIFIED", "USER", user.getId(), null);
        return true;
    }

    @Transactional
    public Optional<ResolvedApiKey> resolveApiKey(String token) {
        return userApiKeyService.resolveApiKey(token);
    }

    private IssuedSecurityToken issueSecurityToken(UserAccount user, UserSecurityTokenType type, Duration ttl) {
        String token = randomToken(32);
        Instant expiresAt = Instant.now().plus(ttl);
        securityTokenRepository.save(new UserSecurityToken(user, type, sha256(token), expiresAt));
        return new IssuedSecurityToken(token, expiresAt);
    }

    private UserSecurityToken requireUsableToken(String token, UserSecurityTokenType type) {
        String tokenHash = sha256(token.trim());
        // Constant-time lookup: always query the database to prevent timing attacks
        UserSecurityToken securityToken = securityTokenRepository
                .findByTokenHashAndTypeAndUsedAtIsNull(tokenHash, type)
                .orElseGet(() -> {
                    // Dummy query to maintain constant time
                    securityTokenRepository.findByTokenHashAndTypeAndUsedAtIsNull("dummy", type);
                    return null;
                });
        if (securityToken == null || !securityToken.isUsable()) {
            sleep(BRUTE_FORCE_DELAY_MILLIS);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    securityToken == null ? "Invalid token" : "Token expired");
        }
        return securityToken;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean verifyTwoFactorOrBackup(UserAccount user, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        if (verifyEmailTwoFactorCode(user, code)) {
            return true;
        }
        if (twoFactorService.verify(user.getTwoFactorSecret(), code)) {
            return true;
        }
        if (twoFactorService.verifyBackupCode(user.getBackupCodes(), code)) {
            user.setBackupCodes(twoFactorService.removeUsedBackupCode(user.getBackupCodes(), code));
            return true;
        }
        return false;
    }

    private void requirePassword(UserAccount user, String password) {
        // OAuth-only accounts have no user-known password; their session identity
        // is sufficient since they authenticated via the OAuth provider.
        if (!user.hasPassword()) {
            return;
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password is incorrect");
        }
    }

    private UserAccount requireUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserAccount requireUserForUpdate(Long userId) {
        return userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }

    private String randomToken(int bytesLength) {
        byte[] bytes = new byte[bytesLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sixDigitCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void recordAccountChange(SecurityEventType type, UserAccount user) {
        try {
            boolean admin = user.getRole() == com.example.KendyDigital.model.user.UserRole.ADMIN
                    || user.getRole() == com.example.KendyDigital.model.user.UserRole.SUPER_ADMIN;
            securitySignalService.record(SecuritySignal
                    .of(type, SecuritySeverity.HIGH, clientIpResolver.resolveCurrent())
                    .user(user.getId())
                    .metadata("admin=" + admin + ";source=selfService")
                    .risk(40)
                    .build());
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not record account change signal {}", type, exception);
        }
    }

    private record IssuedSecurityToken(String token, Instant expiresAt) {
    }
}

