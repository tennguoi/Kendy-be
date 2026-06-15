package com.example.KendyDigital.service.auth;

import java.time.Instant;

public class OAuthTwoFactorRequiredException extends RuntimeException {
    private final String challengeToken;
    private final String email;
    private final Instant expiresAt;
    private final String provider;

    public OAuthTwoFactorRequiredException(String challengeToken, Instant expiresAt, String email, String provider) {
        super("2FA code required");
        this.challengeToken = challengeToken;
        this.email = email;
        this.expiresAt = expiresAt;
        this.provider = provider;
    }

    public String challengeToken() {
        return challengeToken;
    }

    public String email() {
        return email;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public String provider() {
        return provider;
    }
}
