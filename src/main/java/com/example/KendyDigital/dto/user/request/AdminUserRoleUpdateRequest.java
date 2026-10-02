package com.example.KendyDigital.dto.user.request;


import jakarta.validation.constraints.Size;
import com.example.KendyDigital.model.user.UserRole;
import jakarta.validation.constraints.NotNull;

public record AdminUserRoleUpdateRequest(
        @NotNull UserRole role,
        @Size(max = 500) String reason,
        Long version) {

    public AdminUserRoleUpdateRequest(UserRole role, String reason) {
        this(role, reason, null);
    }
}
