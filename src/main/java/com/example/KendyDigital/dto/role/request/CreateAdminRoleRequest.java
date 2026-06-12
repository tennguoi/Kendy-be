package com.example.KendyDigital.dto.role.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record CreateAdminRoleRequest(
        @NotBlank String name,
        String description,
        List<Long> permissionIds) {
}
