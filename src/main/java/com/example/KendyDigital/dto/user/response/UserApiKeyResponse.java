package com.example.KendyDigital.dto.user.response;

import java.time.Instant;
import java.util.List;

import com.example.KendyDigital.model.UserApiKey;

public record UserApiKeyResponse(
        Long id,
        String name,
        String keyPrefix,
        List<String> scopes,
        Instant lastUsedAt,
        Instant revokedAt,
        Instant createdAt) {
    public static UserApiKeyResponse from(UserApiKey key) {
        return new UserApiKeyResponse(
                key.getId(),
                key.getName(),
                key.getKeyPrefix(),
                splitScopes(key.getScopes()),
                key.getLastUsedAt(),
                key.getRevokedAt(),
                key.getCreatedAt());
    }

    private static List<String> splitScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
