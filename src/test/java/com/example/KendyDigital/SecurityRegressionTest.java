package com.example.KendyDigital;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.KendyDigital.dto.auth.request.AuthEmailRequest;
import com.example.KendyDigital.dto.auth.request.AuthForgotPasswordRequest;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.AuthSessionRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserApiKeyRepository;
import com.example.KendyDigital.repository.UserSecurityTokenRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.auth.AuthTokenService;
import com.example.KendyDigital.service.notification.EmailNotificationService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import com.example.KendyDigital.service.security.TwoFactorService;
import com.example.KendyDigital.service.security.UserSecurityServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SecurityRegressionTest {
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private AuthSessionRepository authSessionRepository;
    @Mock private UserSecurityTokenRepository securityTokenRepository;
    @Mock private UserApiKeyRepository userApiKeyRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthTokenService authTokenService;
    @Mock private TwoFactorService twoFactorService;
    @Mock private AuditService auditService;
    @Mock private UserNotificationService userNotificationService;
    @Mock private EmailNotificationService emailNotificationService;

    private UserSecurityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserSecurityServiceImpl(
                userAccountRepository,
                authSessionRepository,
                securityTokenRepository,
                userApiKeyRepository,
                passwordEncoder,
                authTokenService,
                twoFactorService,
                auditService,
                userNotificationService,
                emailNotificationService);
    }

    @Test
    void forgotPasswordNeverReturnsTokenOrRevealsWhetherEmailExists() {
        UserAccount user = new UserAccount("User", "user@example.com", null, "hash");
        when(userAccountRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        var existing = service.forgotPassword(new AuthForgotPasswordRequest("user@example.com"));
        when(userAccountRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        var missing = service.forgotPassword(new AuthForgotPasswordRequest("missing@example.com"));

        assertNull(existing.token());
        assertNull(existing.expiresAt());
        assertEquals(existing.message(), missing.message());
        assertNull(missing.token());
        verify(emailNotificationService).sendPasswordReset(any(), any(), any());
    }

    @Test
    void resendVerificationUsesGenericResponseForMissingEmail() {
        when(userAccountRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        var response = service.resendVerification(new AuthEmailRequest("missing@example.com"));

        assertEquals("If the email exists and is not verified, a verification email has been sent.",
                response.message());
        assertNull(response.token());
        assertNull(response.expiresAt());
    }
}
