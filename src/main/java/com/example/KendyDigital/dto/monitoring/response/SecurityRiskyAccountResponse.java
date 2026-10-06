package com.example.KendyDigital.dto.monitoring.response;

import com.example.KendyDigital.model.user.UserAccount;
import java.time.Instant;

public record SecurityRiskyAccountResponse(
        Long userId,
        String name,
        String email,
        String role,
        String status,
        int failedLoginAttempts,
        boolean locked,
        Instant lockedUntil,
        boolean twoFactorEnabled,
        boolean walletFrozen) {
    public static SecurityRiskyAccountResponse from(UserAccount user) {
        return new SecurityRiskyAccountResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getStatus() == null ? null : user.getStatus().name(),
                user.getFailedLoginAttempts(),
                user.isLocked(),
                user.getLockedUntil(),
                user.isTwoFactorEnabled(),
                user.isWalletFrozen());
    }
}
