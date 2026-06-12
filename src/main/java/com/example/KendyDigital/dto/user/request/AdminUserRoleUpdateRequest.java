package com.example.KendyDigital.dto.user.request;

import com.example.KendyDigital.model.UserRole;

import jakarta.validation.constraints.NotNull;

public record AdminUserRoleUpdateRequest(
        @NotNull UserRole role,
        String reason) {
}
