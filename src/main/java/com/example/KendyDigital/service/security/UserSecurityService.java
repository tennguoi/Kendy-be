package com.example.KendyDigital.service.security;

import com.example.KendyDigital.dto.auth.request.AuthEmailRequest;
import com.example.KendyDigital.dto.auth.request.AuthForgotPasswordRequest;
import com.example.KendyDigital.dto.auth.request.AuthResetPasswordRequest;
import com.example.KendyDigital.dto.auth.request.AuthVerifyEmailRequest;
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
import java.util.List;
import java.util.Optional;

public interface UserSecurityService {
    SecurityTokenResponse forgotPassword(AuthForgotPasswordRequest request);
    AuthUserResponse resetPassword(AuthResetPasswordRequest request);
    SecurityTokenResponse resendVerification(AuthEmailRequest request);
    SecurityTokenResponse sendEmailVerification(UserAccount user);
    AuthUserResponse verifyEmail(AuthVerifyEmailRequest request);
    AuthTokenResponse refresh(String token);
    UserSecurityOverviewResponse overview(Long userId);
    List<com.example.KendyDigital.dto.auth.response.AuthSessionResponse> sessions(Long userId, int page, int size);
    void revokeSession(Long userId, Long sessionId);
    void revokeAllSessions(Long userId);
    TotpSetupResponse setupTwoFactor(Long userId);
    AuthUserResponse enableTwoFactor(Long userId, TwoFactorVerifyRequest request);
    SecurityTokenResponse requestEmailTwoFactorEnable(Long userId);
    AuthUserResponse enableEmailTwoFactor(Long userId, TwoFactorVerifyRequest request);
    AuthUserResponse disableTwoFactor(Long userId, TwoFactorDisableRequest request);
    TotpSetupResponse resetTwoFactor(Long userId, TwoFactorDisableRequest request);
    TotpSetupResponse regenerateBackupCodes(Long userId, TwoFactorDisableRequest request);
    List<UserApiKeyResponse> apiKeys(Long userId, int page, int size);
    UserApiKeyCreatedResponse createApiKey(Long userId, UserApiKeyCreateRequest request);
    void revokeApiKey(Long userId, Long keyId);
    SecurityTokenResponse sendTwoFactorEmailCode(UserAccount user);
    SecurityTokenResponse issueOAuthTwoFactorChallenge(UserAccount user);
    AuthTokenResponse verifyOAuthTwoFactor(String challengeToken, String code);
    boolean verifyEmailTwoFactorCode(UserAccount user, String code);
    Optional<UserAccount> resolveApiKey(String token);
}
