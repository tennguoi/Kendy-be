package com.example.KendyDigital.controller;






import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.KendyDigital.config.AppOAuth2Properties;
import com.example.KendyDigital.service.*;
import com.example.KendyDigital.security.*;
import com.example.KendyDigital.repository.*;
import com.example.KendyDigital.model.*;
import com.example.KendyDigital.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserSecurityService userSecurityService;
    private final AppOAuth2Properties oAuth2Properties;

    public AuthController(AuthService authService, UserSecurityService userSecurityService,
            AppOAuth2Properties oAuth2Properties) {
        this.authService = authService;
        this.userSecurityService = userSecurityService;
        this.oAuth2Properties = oAuth2Properties;
    }

    @PostMapping("/register")
    public AuthUserResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/2fa/email-code")
    public SecurityTokenResponse sendTwoFactorEmailCode(@Valid @RequestBody AuthTwoFactorEmailRequest request) {
        return authService.sendLoginTwoFactorEmailCode(request);
    }

    @PostMapping("/oauth2/2fa/verify")
    public AuthTokenResponse verifyOAuthTwoFactor(@Valid @RequestBody OAuthTwoFactorVerifyRequest request) {
        return userSecurityService.verifyOAuthTwoFactor(request.challengeToken(), request.code());
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return userSecurityService.refresh(extractBearerToken(authorization));
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(name = "Authorization", required = false) String authorization) {
        authService.logout(extractBearerToken(authorization));
    }

    @PostMapping("/forgot-password")
    public SecurityTokenResponse forgotPassword(@Valid @RequestBody AuthForgotPasswordRequest request) {
        return userSecurityService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public AuthUserResponse resetPassword(@Valid @RequestBody AuthResetPasswordRequest request) {
        return userSecurityService.resetPassword(request);
    }

    @PostMapping("/resend-verification")
    public SecurityTokenResponse resendVerification(@Valid @RequestBody AuthEmailRequest request) {
        return userSecurityService.resendVerification(request);
    }

    @PostMapping("/verify-email")
    public AuthUserResponse verifyEmail(@Valid @RequestBody AuthVerifyEmailRequest request) {
        return userSecurityService.verifyEmail(request);
    }

    @GetMapping("/oauth2/providers")
    public Map<String, Object> oauthProviders() {
        List<Map<String, String>> providers = new ArrayList<>();
        if (configured(oAuth2Properties.getGoogle())) {
            providers.add(Map.of("id", "google", "name", "Google", "authorizationUrl", "/oauth2/authorization/google"));
        }
        if (configured(oAuth2Properties.getGithub())) {
            providers.add(Map.of("id", "github", "name", "GitHub", "authorizationUrl", "/oauth2/authorization/github"));
        }
        return Map.of("providers", providers);
    }

    @GetMapping("/me")
    public AuthenticatedUser me(Authentication authentication) {
        return CurrentUser.require(authentication);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token required");
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private boolean configured(AppOAuth2Properties.Client client) {
        return client.getClientId() != null && !client.getClientId().isBlank()
                && client.getClientSecret() != null && !client.getClientSecret().isBlank();
    }
}
