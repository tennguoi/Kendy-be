package com.example.KendyDigital.service;




import com.example.KendyDigital.dto.*;
import com.example.KendyDigital.repository.*;
import com.example.KendyDigital.model.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(12);

    private final AuthSessionRepository authSessionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthTokenService(AuthSessionRepository authSessionRepository) {
        this.authSessionRepository = authSessionRepository;
    }

    @Transactional
    public IssuedToken issue(UserAccount user) {
        String token = randomToken();
        Instant expiresAt = Instant.now().plus(ACCESS_TOKEN_TTL);
        authSessionRepository.save(new AuthSession(sha256(token), user, expiresAt));
        return new IssuedToken(token, expiresAt);
    }

    @Transactional
    public Optional<UserAccount> resolveUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Optional<AuthSession> session = authSessionRepository.findByTokenHashAndRevokedAtIsNull(sha256(token));
        if (session.isEmpty()) {
            return Optional.empty();
        }

        AuthSession authSession = session.get();
        if (authSession.getExpiresAt().isBefore(Instant.now()) || authSession.getUser().getStatus() != UserStatus.ACTIVE) {
            authSession.revoke();
            return Optional.empty();
        }

        authSession.markUsed();
        return Optional.of(authSession.getUser());
    }

    @Transactional
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        authSessionRepository.findByTokenHashAndRevokedAtIsNull(sha256(token))
                .ifPresent(AuthSession::revoke);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
