package com.example.KendyDigital.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.AuthTokenResponse;
import com.example.KendyDigital.dto.AuthUserResponse;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.repository.UserAccountRepository;

@Service
public class OAuth2AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final AuditService auditService;
    private final UserNotificationService userNotificationService;
    private final RestClient restClient = RestClient.create();
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuth2AuthService(UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService,
            AuditService auditService,
            UserNotificationService userNotificationService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.auditService = auditService;
        this.userNotificationService = userNotificationService;
    }

    @Transactional
    public AuthTokenResponse login(String registrationId, Map<String, Object> attributes, String accessToken) {
        OAuthProfile profile = profile(registrationId, attributes, accessToken);
        UserAccount user = userAccountRepository
                .findByOauthProviderAndOauthProviderId(profile.provider(), profile.providerId())
                .or(() -> userAccountRepository.findByEmailIgnoreCase(profile.email()))
                .orElseGet(() -> userAccountRepository.save(new UserAccount(
                        profile.name(),
                        profile.email(),
                        null,
                        passwordEncoder.encode(randomPassword()))));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is locked");
        }
        user.linkOAuth(profile.provider(), profile.providerId(), profile.avatarUrl());
        if (profile.emailVerified()) {
            user.verifyEmail();
        }

        AuthTokenService.IssuedToken issuedToken = authTokenService.issue(user);
        auditService.recordSystem("USER_OAUTH_LOGIN", "USER", user.getId(), "provider=" + profile.provider());
        userNotificationService.create(user.getId(),
                "New " + profile.providerDisplayName() + " login",
                "Your account signed in with " + profile.providerDisplayName() + ".",
                "SECURITY",
                "/account/security");
        return new AuthTokenResponse(issuedToken.token(), issuedToken.expiresAt(), AuthUserResponse.from(user));
    }

    private OAuthProfile profile(String registrationId, Map<String, Object> attributes, String accessToken) {
        String provider = registrationId.toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "google" -> googleProfile(attributes);
            case "github" -> githubProfile(attributes, accessToken);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported OAuth provider");
        };
    }

    private OAuthProfile googleProfile(Map<String, Object> attributes) {
        String email = requiredString(attributes, "email", "Google account email is required");
        String providerId = requiredString(attributes, "sub", "Google subject is required");
        return new OAuthProfile(
                "google",
                "Google",
                providerId,
                email.toLowerCase(Locale.ROOT),
                optionalString(attributes, "name", email),
                optionalString(attributes, "picture", null),
                Boolean.TRUE.equals(attributes.get("email_verified")));
    }

    private OAuthProfile githubProfile(Map<String, Object> attributes, String accessToken) {
        String providerId = String.valueOf(attributes.get("id"));
        if (providerId == null || providerId.isBlank() || "null".equals(providerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GitHub account id is required");
        }
        String login = optionalString(attributes, "login", providerId);
        String email = optionalString(attributes, "email", null);
        boolean emailVerified = true;
        if ((email == null || email.isBlank()) && accessToken != null && !accessToken.isBlank()) {
            GitHubEmail githubEmail = fetchGitHubEmail(accessToken);
            email = githubEmail.email();
            emailVerified = githubEmail.verified();
        }
        if (email == null || email.isBlank()) {
            email = githubFallbackEmail(providerId, login);
            emailVerified = false;
        }
        String name = optionalString(attributes, "name", null);
        if (name == null || name.isBlank()) {
            name = login;
        }
        return new OAuthProfile(
                "github",
                "GitHub",
                providerId,
                email.toLowerCase(Locale.ROOT),
                name,
                optionalString(attributes, "avatar_url", null),
                emailVerified);
    }

    private GitHubEmail fetchGitHubEmail(String accessToken) {
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "KendyDigital")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (emails == null) {
                return new GitHubEmail(null, false);
            }
            return emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("primary"))
                            && Boolean.TRUE.equals(email.get("verified")))
                    .findFirst()
                    .or(() -> emails.stream().filter(email -> Boolean.TRUE.equals(email.get("verified"))).findFirst())
                    .map(email -> new GitHubEmail(optionalString(email, "email", null),
                            Boolean.TRUE.equals(email.get("verified"))))
                    .orElse(new GitHubEmail(null, false));
        } catch (Exception exception) {
            return new GitHubEmail(null, false);
        }
    }

    private String requiredString(Map<String, Object> attributes, String key, String message) {
        String value = optionalString(attributes, key, null);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    private String optionalString(Map<String, Object> attributes, String key, String fallback) {
        Object value = attributes.get(key);
        return value == null ? fallback : value.toString();
    }

    private String githubFallbackEmail(String providerId, String login) {
        String normalizedLogin = login == null ? "github-user" : login.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-");
        if (normalizedLogin.isBlank()) {
            normalizedLogin = "github-user";
        }
        return providerId + "+" + normalizedLogin + "@users.noreply.github.com";
    }

    private String randomPassword() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record OAuthProfile(
            String provider,
            String providerDisplayName,
            String providerId,
            String email,
            String name,
            String avatarUrl,
            boolean emailVerified) {
    }

    private record GitHubEmail(String email, boolean verified) {
    }
}
