package com.example.KendyDigital.service.auth;

import com.example.KendyDigital.config.AppSecurityProperties;
import com.example.KendyDigital.model.auth.AuthSession;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.repository.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenServiceImpl  implements AuthTokenService{
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(12);
    private static final String TOKEN_COOKIE_NAME = "access_token";

    private final AuthSessionRepository authSessionRepository;
    private final int maxActiveSessions;
    private final boolean enableHttpOnlyCookie;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthTokenServiceImpl(AuthSessionRepository authSessionRepository,
            @Value("${app.security.max-active-sessions:3}") int maxActiveSessions,
            AppSecurityProperties securityProperties) {
        this.authSessionRepository = authSessionRepository;
        this.maxActiveSessions = Math.max(1, maxActiveSessions);
        this.enableHttpOnlyCookie = securityProperties.isEnableHttpOnlyCookie();
    }

    @Transactional
    public IssuedToken issue(UserAccount user, HttpServletResponse response) {
        var activeSessions = authSessionRepository
                .findAllByUser_IdAndRevokedAtIsNullOrderByCreatedAtAsc(user.getId());
        int sessionsToRevoke = activeSessions.size() - maxActiveSessions + 1;
        for (int index = 0; index < sessionsToRevoke; index++) {
            activeSessions.get(index).revoke();
        }
        String token = randomToken();
        Instant expiresAt = Instant.now().plus(ACCESS_TOKEN_TTL);
        authSessionRepository.save(new AuthSession(sha256(token), user, expiresAt));

        if (enableHttpOnlyCookie && response != null) {
            Cookie cookie = new Cookie(TOKEN_COOKIE_NAME, token);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge((int) ACCESS_TOKEN_TTL.getSeconds());
            cookie.setAttribute("SameSite", "Strict");
            response.addCookie(cookie);
        }

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
    public void revoke(String token, HttpServletResponse response) {
        if (token == null || token.isBlank()) {
            return;
        }
        authSessionRepository.findByTokenHashAndRevokedAtIsNull(sha256(token))
                .ifPresent(AuthSession::revoke);

        if (enableHttpOnlyCookie && response != null) {
            Cookie cookie = new Cookie(TOKEN_COOKIE_NAME, "");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            cookie.setAttribute("SameSite", "Strict");
            response.addCookie(cookie);
        }
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
}
