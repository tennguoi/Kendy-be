package com.example.KendyDigital.dto.monitoring.response;

import com.example.KendyDigital.model.user.UserApiKey;
import java.time.Instant;

public record SecurityApiKeyResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String name,
        String keyPrefix,
        String scopes,
        Instant createdAt,
        Instant lastUsedAt) {
    public static SecurityApiKeyResponse from(UserApiKey key) {
        return new SecurityApiKeyResponse(
                key.getId(),
                key.getUser().getId(),
                key.getUser().getName(),
                key.getUser().getEmail(),
                key.getName(),
                key.getKeyPrefix(),
                key.getScopes(),
                key.getCreatedAt(),
                key.getLastUsedAt());
    }
}
