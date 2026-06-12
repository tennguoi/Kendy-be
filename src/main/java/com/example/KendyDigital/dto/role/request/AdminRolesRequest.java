package com.example.KendyDigital.dto.role.request;

import com.example.KendyDigital.model.UserRole;

import jakarta.validation.constraints.NotNull;

public record AdminRolesRequest(
        @NotNull UserRole role,
        String reason) {
}
