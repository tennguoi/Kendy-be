package com.example.KendyDigital.dto;


import com.example.KendyDigital.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuthUserResponse(
        Long id,
        UUID publicId,
        String name,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        BigDecimal balance,
        boolean twoFactorEnabled,
        Instant emailVerifiedAt,
        String oauthProvider,
        String avatarUrl) {
    public static AuthUserResponse from(UserAccount user) {
        return new AuthUserResponse(
                user.getId(),
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
                user.getAvatarUrl());
    }
}
