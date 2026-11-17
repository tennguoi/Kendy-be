package com.example.KendyDigital.dto.auth.response;

import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.model.user.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        BigDecimal balance,
        boolean twoFactorEnabled,
        Instant emailVerifiedAt,
        String oauthProvider,
        String avatarUrl,
        String locale,
        boolean hasPassword) {
    public static AuthUserResponse from(UserAccount user) {
        return new AuthUserResponse(
                user.getPublicId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getBalance(),
                user.isTwoFactorEnabled(),
                user.getEmailVerifiedAt(),
                user.getOauthProvider(),
                user.getAvatarUrl(),
                user.getLocale(),
                user.hasPassword());
    }
}
