package com.example.KendyDigital.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.auth.response.AuthUserResponse;
import com.example.KendyDigital.dto.user.request.ChangePasswordRequest;
import com.example.KendyDigital.dto.auth.response.TotpSetupResponse;
import com.example.KendyDigital.dto.auth.request.TwoFactorDisableRequest;
import com.example.KendyDigital.dto.auth.request.TwoFactorVerifyRequest;
import com.example.KendyDigital.dto.user.request.UpdateProfileRequest;
import com.example.KendyDigital.dto.auth.response.SecurityTokenResponse;
import com.example.KendyDigital.dto.user.request.UserApiKeyCreateRequest;
import com.example.KendyDigital.dto.user.response.UserApiKeyCreatedResponse;
import com.example.KendyDigital.dto.user.response.UserApiKeyResponse;
import com.example.KendyDigital.dto.user.response.UserDashboardResponse;
import com.example.KendyDigital.dto.user.response.UserSecurityOverviewResponse;
import com.example.KendyDigital.dto.auth.response.AuthSessionResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.UserSecurityService;
import com.example.KendyDigital.service.UserProfileService;

import jakarta.validation.Valid;

@RestController
public class MeController {
    private final UserProfileService userProfileService;
    private final UserSecurityService userSecurityService;

    public MeController(UserProfileService userProfileService, UserSecurityService userSecurityService) {
        this.userProfileService = userProfileService;
        this.userSecurityService = userSecurityService;
    }

    @GetMapping("/api/me")
    public AuthUserResponse me(Authentication authentication) {
        return userProfileService.getProfile(CurrentUser.require(authentication).userId());
    }

    @PutMapping("/api/me")
    public AuthUserResponse updateProfile(Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.updateProfile(CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/me/change-password")
    public AuthUserResponse changePassword(Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        return userProfileService.changePassword(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/me/dashboard")
    public UserDashboardResponse dashboard(Authentication authentication) {
        return userProfileService.dashboard(CurrentUser.require(authentication).userId());
    }

    @GetMapping("/api/me/security")
    public UserSecurityOverviewResponse security(Authentication authentication) {
        return userSecurityService.overview(CurrentUser.require(authentication).userId());
    }

    @GetMapping("/api/me/sessions")
    public List<AuthSessionResponse> sessions(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userSecurityService.sessions(CurrentUser.require(authentication).userId(), page, size);
    }

    @DeleteMapping("/api/me/sessions/{sessionId}")
    public void revokeSession(Authentication authentication, @PathVariable Long sessionId) {
        userSecurityService.revokeSession(CurrentUser.require(authentication).userId(), sessionId);
    }

    @DeleteMapping("/api/me/sessions")
    public void revokeAllSessions(Authentication authentication) {
        userSecurityService.revokeAllSessions(CurrentUser.require(authentication).userId());
    }

    @PostMapping("/api/me/2fa/setup")
    public TotpSetupResponse setupTwoFactor(Authentication authentication) {
        return userSecurityService.setupTwoFactor(CurrentUser.require(authentication).userId());
    }

    @PostMapping("/api/me/2fa/enable")
    public AuthUserResponse enableTwoFactor(Authentication authentication,
            @Valid @RequestBody TwoFactorVerifyRequest request) {
        return userSecurityService.enableTwoFactor(CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/me/2fa/email-code")
    public SecurityTokenResponse sendTwoFactorEnableEmailCode(Authentication authentication) {
        return userSecurityService.requestEmailTwoFactorEnable(CurrentUser.require(authentication).userId());
    }

    @PostMapping("/api/me/2fa/enable-email")
    public AuthUserResponse enableEmailTwoFactor(Authentication authentication,
            @Valid @RequestBody TwoFactorVerifyRequest request) {
        return userSecurityService.enableEmailTwoFactor(CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/me/2fa/disable")
    public AuthUserResponse disableTwoFactor(Authentication authentication,
            @Valid @RequestBody TwoFactorDisableRequest request) {
        return userSecurityService.disableTwoFactor(CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/me/2fa/reset")
    public TotpSetupResponse resetTwoFactor(Authentication authentication,
            @Valid @RequestBody TwoFactorDisableRequest request) {
        return userSecurityService.resetTwoFactor(CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/me/2fa/backup-codes")
    public TotpSetupResponse regenerateBackupCodes(Authentication authentication,
            @Valid @RequestBody TwoFactorDisableRequest request) {
        return userSecurityService.regenerateBackupCodes(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/me/api-keys")
    public List<UserApiKeyResponse> apiKeys(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userSecurityService.apiKeys(CurrentUser.require(authentication).userId(), page, size);
    }

    @PostMapping("/api/me/api-keys")
    public UserApiKeyCreatedResponse createApiKey(Authentication authentication,
            @Valid @RequestBody UserApiKeyCreateRequest request) {
        return userSecurityService.createApiKey(CurrentUser.require(authentication).userId(), request);
    }

    @DeleteMapping("/api/me/api-keys/{keyId}")
    public void revokeApiKey(Authentication authentication, @PathVariable Long keyId) {
        userSecurityService.revokeApiKey(CurrentUser.require(authentication).userId(), keyId);
    }
}
