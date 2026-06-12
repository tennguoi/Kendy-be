package com.example.KendyDigital.dto.role.response;

import java.time.Instant;
import java.util.List;

import com.example.KendyDigital.model.AdminRole;

public record AdminRoleResponse(
        Long id,
        String name,
        String description,
        boolean system,
        List<String> permissions,
        Instant createdAt) {
    public static AdminRoleResponse from(AdminRole role, List<String> permissions) {
        return new AdminRoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isSystem(),
                permissions,
                role.getCreatedAt());
    }
}
