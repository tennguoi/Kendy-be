package com.example.KendyDigital.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.auth.request.AuthLoginRequest;
import com.example.KendyDigital.dto.auth.request.AuthRegisterRequest;
import com.example.KendyDigital.dto.auth.request.AuthTwoFactorEmailRequest;
import com.example.KendyDigital.dto.auth.response.AuthTokenResponse;
import com.example.KendyDigital.dto.auth.response.AuthUserResponse;
import com.example.KendyDigital.dto.auth.response.SecurityTokenResponse;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserRole;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.repository.SystemSettingRepository;
import com.example.KendyDigital.repository.UserAccountRepository;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final TwoFactorService twoFactorService;
    private final UserSecurityService userSecurityService;
    private final SystemSettingRepository systemSettingRepository;

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService, TwoFactorService twoFactorService,
            UserSecurityService userSecurityService,
            SystemSettingRepository systemSettingRepository) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.twoFactorService = twoFactorService;
        this.userSecurityService = userSecurityService;
        this.systemSettingRepository = systemSettingRepository;
    }

    @Transactional
    public AuthUserResponse register(AuthRegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserAccount user = new UserAccount(
                request.name().trim(),
                email,
                blankToNull(request.phone()),
                passwordEncoder.encode(request.password()));
        UserAccount saved = userAccountRepository.save(user);
        userSecurityService.sendEmailVerification(saved);
        return AuthUserResponse.from(saved);
    }

    @Transactional
    public AuthTokenResponse login(AuthLoginRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        if (requiresLoginTwoFactor(user)) {
            if (request.twoFactorCode() != null && !request.twoFactorCode().isBlank()) {
                if (twoFactorService.verify(user.getTwoFactorSecret(), request.twoFactorCode())) {
                    return issueToken(user);
                }
                if (userSecurityService.verifyEmailTwoFactorCode(user, request.twoFactorCode())) {
                    return issueToken(user);
                }
                if (twoFactorService.verifyBackupCode(user.getBackupCodes(), request.twoFactorCode())) {
                    user.setBackupCodes(twoFactorService.removeUsedBackupCode(user.getBackupCodes(),
                            request.twoFactorCode()));
                    userAccountRepository.save(user);
                    return issueToken(user);
                }
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid 2FA code");
            } else {
                userSecurityService.sendTwoFactorEmailCode(user);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "2FA code required; email code sent");
            }
        }

        return issueToken(user);
    }

    @Transactional
    public SecurityTokenResponse sendLoginTwoFactorEmailCode(AuthTwoFactorEmailRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
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

    private AuthTokenResponse issueToken(UserAccount user) {
        AuthTokenService.IssuedToken issuedToken = authTokenService.issue(user);
        return new AuthTokenResponse(issuedToken.token(), issuedToken.expiresAt(), AuthUserResponse.from(user));
    }

    public void logout(String token) {
        authTokenService.revoke(token);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
