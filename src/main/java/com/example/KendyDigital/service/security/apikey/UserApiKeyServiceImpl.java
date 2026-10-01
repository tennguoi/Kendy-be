package com.example.KendyDigital.service.security.apikey;

import com.example.KendyDigital.dto.user.request.UserApiKeyCreateRequest;
import com.example.KendyDigital.dto.user.response.UserApiKeyCreatedResponse;
import com.example.KendyDigital.dto.user.response.UserApiKeyResponse;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserApiKey;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserApiKeyRepository;
import com.example.KendyDigital.security.ResolvedApiKey;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserApiKeyServiceImpl implements UserApiKeyService {
    private static final String API_KEY_PREFIX = "kdy_";

    private final UserApiKeyRepository userApiKeyRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final UserNotificationService userNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserApiKeyServiceImpl(
            UserApiKeyRepository userApiKeyRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService,
            UserNotificationService userNotificationService) {
        this.userApiKeyRepository = userApiKeyRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.userNotificationService = userNotificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserApiKeyResponse> apiKeys(Long userId, int page, int size) {
        return userApiKeyRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(UserApiKeyResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public UserApiKeyCreatedResponse createApiKey(Long userId, UserApiKeyCreateRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String token = API_KEY_PREFIX + randomToken(36);
        String scopes = request.scopes() == null ? "" : request.scopes().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
        UserApiKey apiKey = userApiKeyRepository.save(new UserApiKey(
                user,
                request.name().trim(),
                token.substring(0, Math.min(token.length(), 12)),
                sha256(token),
                scopes));
        auditService.recordSystem("USER_API_KEY_CREATED", "USER_API_KEY", apiKey.getId(), "userId=" + userId);
        userNotificationService.create(user.getId(), "API key created",
                "API key '" + apiKey.getName() + "' was created.", "SECURITY", "/account/security");
        return new UserApiKeyCreatedResponse(UserApiKeyResponse.from(apiKey), token);
    }

    @Override
    @Transactional
    public void revokeApiKey(Long userId, Long keyId) {
        UserApiKey key = userApiKeyRepository.findByIdAndUser_Id(keyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
        key.revoke();
        auditService.recordSystem("USER_API_KEY_REVOKED", "USER_API_KEY", key.getId(), "userId=" + userId);
        userNotificationService.create(userId, "API key revoked",
                "API key '" + key.getName() + "' was revoked.", "SECURITY", "/account/security");
    }

    @Override
    @Transactional
    public void revokeAllApiKeys(Long userId) {
        userApiKeyRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500))
                .forEach(apiKey -> {
                    if (apiKey.getRevokedAt() == null) {
                        apiKey.revoke();
                    }
                });
    }

    @Override
    @Transactional
    public Optional<ResolvedApiKey> resolveApiKey(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<UserApiKey> apiKey = userApiKeyRepository.findByKeyHashAndRevokedAtIsNull(sha256(token.trim()));
        if (apiKey.isEmpty()) {
            return Optional.empty();
        }
        UserApiKey key = apiKey.get();
        if (key.getUser().getStatus() != UserStatus.ACTIVE) {
            return Optional.empty();
        }
        key.markUsed();
        Set<String> scopes = Arrays.stream(Optional.ofNullable(key.getScopes()).orElse("").split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        return Optional.of(new ResolvedApiKey(key.getUser(), scopes));
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveKeys(Long userId) {
        return userApiKeyRepository.countByUser_IdAndRevokedAtIsNull(userId);
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }

    private String randomToken(int bytesLength) {
        byte[] bytes = new byte[bytesLength];
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
