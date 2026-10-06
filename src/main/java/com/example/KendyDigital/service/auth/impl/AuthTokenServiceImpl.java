package com.example.KendyDigital.service.auth.impl;

import com.example.KendyDigital.service.auth.*;

import com.example.KendyDigital.common.ClientIpResolver;
import com.example.KendyDigital.config.AppSecurityProperties;
import com.example.KendyDigital.model.auth.AuthSession;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.repository.*;
import com.example.KendyDigital.service.security.monitor.GeoIpService;
import com.example.KendyDigital.service.security.monitor.SecuritySignal;
import com.example.KendyDigital.service.security.monitor.SecuritySignalService;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuthTokenServiceImpl  implements AuthTokenService{
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(12);
    private static final String TOKEN_COOKIE_NAME = "access_token";

    private final AuthSessionRepository authSessionRepository;
    private final UserAccountRepository userAccountRepository;
    private final int maxActiveSessions;
    private final boolean enableHttpOnlyCookie;
    private final ClientIpResolver clientIpResolver;
    private final GeoIpService geoIpService;
    private final SecuritySignalService securitySignalService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthTokenServiceImpl(AuthSessionRepository authSessionRepository,
            UserAccountRepository userAccountRepository,
            @Value("${app.security.max-active-sessions:3}") int maxActiveSessions,
            AppSecurityProperties securityProperties,
            ClientIpResolver clientIpResolver,
            GeoIpService geoIpService,
            @Lazy SecuritySignalService securitySignalService) {
        this.authSessionRepository = authSessionRepository;
        this.userAccountRepository = userAccountRepository;
        this.maxActiveSessions = Math.max(1, maxActiveSessions);
        this.enableHttpOnlyCookie = securityProperties.isEnableHttpOnlyCookie();
        this.clientIpResolver = clientIpResolver;
        this.geoIpService = geoIpService;
        this.securitySignalService = securitySignalService;
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
        AuthSession session = new AuthSession(sha256(token), user, expiresAt);
        String ip = clientIpResolver.resolveCurrent();
        String userAgent = currentUserAgent();
        GeoIpService.GeoInfo geo = geoIpService.lookup(ip);
        session.setCreatedIp(ip);
        session.setLastIp(ip);
        session.setUserAgent(userAgent);
        session.setDeviceHash(deviceHash(userAgent));
        session.setCountry(geo.country());
        session.setAsn(geo.asn());
        authSessionRepository.save(session);

        user.recordLoginContext(Instant.now(), ip, geo.country());
        userAccountRepository.save(user);

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

        Optional<AuthSession> session = authSessionRepository.findByTokenHash(sha256(token));
        if (session.isEmpty()) {
            return Optional.empty();
        }

        AuthSession authSession = session.get();
        String ip = clientIpResolver.resolveCurrent();
        if (authSession.getRevokedAt() != null
                || authSession.getExpiresAt().isBefore(Instant.now())
                || authSession.getUser().getStatus() != UserStatus.ACTIVE) {
            if (authSession.getRevokedAt() != null) {
                emit(SecuritySignal.of(SecurityEventType.REVOKED_TOKEN_USED, SecuritySeverity.HIGH, ip)
                        .user(authSession.getUser().getId())
                        .session(authSession.getId())
                        .metadata("reason=revokedTokenReuse")
                        .risk(50)
                        .build());
            }
            authSession.revoke();
            return Optional.empty();
        }

        detectDeviceChange(authSession, ip);
        authSession.markUsed(ip);
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

    private void detectDeviceChange(AuthSession session, String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        String newDeviceHash = deviceHash(currentUserAgent());
        boolean ipChanged = session.getLastIp() != null && !session.getLastIp().equals(ip);
        boolean deviceChanged = session.getDeviceHash() != null && newDeviceHash != null
                && !session.getDeviceHash().equals(newDeviceHash);
        if (ipChanged && deviceChanged) {
            emit(SecuritySignal.of(SecurityEventType.SESSION_HIJACK_SUSPECTED, SecuritySeverity.HIGH, ip)
                    .user(session.getUser().getId())
                    .session(session.getId())
                    .metadata("previousIp=" + session.getLastIp() + ";reason=ipAndDeviceChanged")
                    .risk(60)
                    .build());
        }
    }

    private void emit(SecuritySignal signal) {
        try {
            securitySignalService.record(signal);
        } catch (RuntimeException ignored) {
        }
    }

    private String currentUserAgent() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                String userAgent = servletAttributes.getRequest().getHeader("User-Agent");
                return userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 512));
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private String deviceHash(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(userAgent.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return null;
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
