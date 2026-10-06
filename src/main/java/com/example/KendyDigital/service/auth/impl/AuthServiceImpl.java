package com.example.KendyDigital.service.auth.impl;

import com.example.KendyDigital.service.auth.*;

import com.example.KendyDigital.common.ClientIpResolver;
import com.example.KendyDigital.dto.auth.request.AuthLoginRequest;
import com.example.KendyDigital.dto.auth.request.AuthRegisterRequest;
import com.example.KendyDigital.dto.auth.request.AuthTwoFactorEmailRequest;
import com.example.KendyDigital.dto.auth.response.AuthTokenResponse;
import com.example.KendyDigital.dto.auth.response.AuthUserResponse;
import com.example.KendyDigital.dto.auth.response.SecurityTokenResponse;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.repository.SystemSettingRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.security.TwoFactorService;
import com.example.KendyDigital.service.security.UserSecurityService;
import com.example.KendyDigital.service.security.monitor.SecuritySignal;
import com.example.KendyDigital.service.security.monitor.SecuritySignalService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl  implements AuthService{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String DUMMY_PASSWORD_HASH = new BCryptPasswordEncoder().encode("kendy-timing-equalizer");

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final TwoFactorService twoFactorService;
    private final UserSecurityService userSecurityService;
    private final SystemSettingRepository systemSettingRepository;
    private final SecuritySignalService securitySignalService;
    private final ClientIpResolver clientIpResolver;

    public AuthServiceImpl(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService, TwoFactorService twoFactorService,
            UserSecurityService userSecurityService,
            SystemSettingRepository systemSettingRepository,
            SecuritySignalService securitySignalService,
            ClientIpResolver clientIpResolver) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.twoFactorService = twoFactorService;
        this.userSecurityService = userSecurityService;
        this.systemSettingRepository = systemSettingRepository;
        this.securitySignalService = securitySignalService;
        this.clientIpResolver = clientIpResolver;
    }

    @Transactional
    public AuthUserResponse register(AuthRegisterRequest request, String acceptLanguage) {
        LOGGER.info("=== REGISTER START === email={}, name={}", request.email(), request.name());
        String email = normalizeEmail(request.email());
        var existing = userAccountRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            UserAccount existingUser = existing.get();
            LOGGER.info("Existing user found: userId={}, emailVerifiedAt={}, oauthProvider={}, hasPassword={}", 
                existingUser.getId(), existingUser.getEmailVerifiedAt(), existingUser.getOauthProvider(), existingUser.hasPassword());
            // If the existing account was created via OAuth, hint the user
            if (existingUser.getOauthProvider() != null && !existingUser.hasPassword()) {
                String provider = existingUser.getOauthProvider().substring(0, 1).toUpperCase()
                        + existingUser.getOauthProvider().substring(1);
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Email này đã được đăng ký qua " + provider
                                + ". Vui lòng đăng nhập với " + provider
                                + " hoặc dùng 'Quên mật khẩu'.");
            }
            // If account was registered previously but NOT yet verified, allow updating info and resend verification
            if (existingUser.getEmailVerifiedAt() == null) {
                LOGGER.info("Existing unverified user - updating info and resending verification");
                existingUser.setName(request.name().trim());
                existingUser.setPhone(blankToNull(request.phone()));
                existingUser.changePasswordHash(passwordEncoder.encode(request.password()));
                existingUser.setStatus(UserStatus.PENDING_VERIFY);
                existingUser.setLocale(resolveLocale(acceptLanguage));
                UserAccount saved = userAccountRepository.save(existingUser);
                LOGGER.info("Calling userSecurityService.sendEmailVerification for existing user");
                userSecurityService.sendEmailVerification(saved);
                return AuthUserResponse.from(saved);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserAccount user = new UserAccount(
                request.name().trim(),
                email,
                blankToNull(request.phone()),
                passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.PENDING_VERIFY);
        user.setLocale(resolveLocale(acceptLanguage));
        UserAccount saved = userAccountRepository.save(user);
        LOGGER.info("New user created: userId={}, calling sendEmailVerification", saved.getId());
        recordSignal(SecuritySignal.of(SecurityEventType.REGISTER_BURST, SecuritySeverity.INFO, clientIp())
                .user(saved.getId())
                .request("POST", "/api/auth/register")
                .metadata("emailHash=" + emailHash(email))
                .risk(5)
                .build());
        userSecurityService.sendEmailVerification(saved);
        LOGGER.info("=== REGISTER COMPLETED === userId={}", saved.getId());
        return AuthUserResponse.from(saved);
    }

    @Transactional
    public AuthTokenResponse login(AuthLoginRequest request, String acceptLanguage, jakarta.servlet.http.HttpServletResponse response) {
        String email = normalizeEmail(request.email());
        Optional<UserAccount> found = userAccountRepository.findByEmailIgnoreCase(email);
        if (found.isEmpty()) {
            // Constant-ish time: run bcrypt to blunt user-enumeration via response timing.
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            recordSignal(SecuritySignal.of(SecurityEventType.LOGIN_UNKNOWN_EMAIL, SecuritySeverity.LOW, clientIp())
                    .request("POST", "/api/auth/login")
                    .metadata("emailHash=" + emailHash(email))
                    .risk(40)
                    .build());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        UserAccount user = found.get();

        if (user.isLocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userAccountRepository.save(user);
            boolean locked = user.isLocked();
            recordSignal(SecuritySignal.of(
                            locked ? SecurityEventType.ACCOUNT_LOCKED : SecurityEventType.LOGIN_FAILED,
                            locked ? SecuritySeverity.MEDIUM : SecuritySeverity.LOW, clientIp())
                    .user(user.getId())
                    .request("POST", "/api/auth/login")
                    .metadata("emailHash=" + emailHash(email) + ";reason=badPassword")
                    .risk(20)
                    .build());
            // If this is an OAuth-only account (no user-set password), hint them to use OAuth
            if (!user.hasPassword() && user.getOauthProvider() != null) {
                String provider = user.getOauthProvider().substring(0, 1).toUpperCase()
                        + user.getOauthProvider().substring(1);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "This account was created via " + provider
                                + ". Please log in with " + provider
                                + " or use 'Forgot Password' to set a new password.");
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (user.getStatus() == UserStatus.PENDING_VERIFY) {
            userSecurityService.sendEmailVerification(user);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "EMAIL_NOT_VERIFIED");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        user.setLocale(resolveLocale(acceptLanguage));

        if (requiresLoginTwoFactor(user)) {
            if (user.isTwoFactorLocked()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            if (request.twoFactorCode() != null && !request.twoFactorCode().isBlank()) {
                if (twoFactorService.verify(user.getTwoFactorSecret(), request.twoFactorCode())) {
                    return issueToken(user, response);
                }
                if (userSecurityService.verifyEmailTwoFactorCode(user, request.twoFactorCode())) {
                    return issueToken(user, response);
                }
                if (twoFactorService.verifyBackupCode(user.getBackupCodes(), request.twoFactorCode())) {
                    user.setBackupCodes(twoFactorService.removeUsedBackupCode(user.getBackupCodes(),
                            request.twoFactorCode()));
                    userAccountRepository.save(user);
                    return issueToken(user, response);
                }
                user.recordFailedTwoFactor();
                userAccountRepository.save(user);
                recordSignal(SecuritySignal.of(SecurityEventType.TWO_FACTOR_FAILED, SecuritySeverity.MEDIUM, clientIp())
                        .user(user.getId())
                        .request("POST", "/api/auth/login")
                        .metadata("passwordOk=true")
                        .risk(30)
                        .build());
                // Unified with bad-password to avoid confirming the password is correct.
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            } else {
                userSecurityService.sendTwoFactorEmailCode(user);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "2FA code required; email code sent");
            }
        }

        return issueToken(user, response);
    }

    @Transactional
    public SecurityTokenResponse sendLoginTwoFactorEmailCode(AuthTwoFactorEmailRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (user.isLocked()) {
            // Use generic message to prevent account enumeration via lockout status
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userAccountRepository.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFY) {
            userSecurityService.sendEmailVerification(user);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "EMAIL_NOT_VERIFIED");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        if (!requiresLoginTwoFactor(user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "2FA is not enabled");
        }
        return userSecurityService.sendTwoFactorEmailCode(user);
    }

    private boolean requiresLoginTwoFactor(UserAccount user) {
        if (user.isTwoFactorEnabled()) {
            return true;
        }
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.SUPER_ADMIN) {
            return false;
        }
        return systemSettingRepository.findById("admin_2fa_required")
                .map(setting -> "true".equalsIgnoreCase(setting.getValue()))
                .orElse(false);
    }

    private AuthTokenResponse issueToken(UserAccount user, jakarta.servlet.http.HttpServletResponse response) {
        user.recordSuccessfulLogin();
        userAccountRepository.save(user);
        AuthTokenService.IssuedToken issuedToken = authTokenService.issue(user, response);
        recordSignal(SecuritySignal.of(SecurityEventType.LOGIN_SUCCESS, SecuritySeverity.INFO, clientIp())
                .user(user.getId())
                .request("POST", "/api/auth/login")
                .risk(0)
                .build());
        return new AuthTokenResponse(issuedToken.token(), issuedToken.expiresAt(), AuthUserResponse.from(user));
    }

    public void logout(String token, jakarta.servlet.http.HttpServletResponse response) {
        authTokenService.revoke(token, response);
    }

    private String clientIp() {
        return clientIpResolver.resolveCurrent();
    }

    private void recordSignal(SecuritySignal signal) {
        try {
            securitySignalService.record(signal);
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not record auth security signal", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String emailHash(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(email.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return "unknown";
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String resolveLocale(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return "vi";
        }
        String lang = acceptLanguage.split(",")[0].trim().toLowerCase(Locale.ROOT);
        if (lang.startsWith("en")) {
            return "en";
        }
        if (lang.startsWith("vi")) {
            return "vi";
        }
        return "vi";
    }
}
