package com.example.KendyDigital.dto.user.response;

import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.model.user.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        Long id,
        UUID publicId,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        BigDecimal balance,
        boolean twoFactorEnabled,
        Instant createdAt,
        Instant updatedAt,
        String avatarUrl
        ) {
    public static AdminUserResponse from(UserAccount user) {
        return new AdminUserResponse(
                user.getId(),
                user.getPublicId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getBalance(),
                user.isTwoFactorEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getAvatarUrl()
        );
    }
}
