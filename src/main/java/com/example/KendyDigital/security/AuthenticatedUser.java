package com.example.KendyDigital.security;

import com.example.KendyDigital.model.user.UserRole;

public record AuthenticatedUser(
        Long userId,
        String email,
        UserRole role) {
}
