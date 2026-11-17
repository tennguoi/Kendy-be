package com.example.KendyDigital.dto.role.response;

import com.example.KendyDigital.model.admin.AdminRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminRoleResponse(
        UUID id,
        String name,
        String description,
        boolean system,
        List<String> permissions,
        Instant createdAt,
        Long version) {

    public AdminRoleResponse(UUID id, String name, String description, boolean system,
            List<String> permissions, Instant createdAt) {
        this(id, name, description, system, permissions, createdAt, 0L);
    }

    public static AdminRoleResponse from(AdminRole role, List<String> permissions) {
        return new AdminRoleResponse(
                role.getPublicId(),
                role.getName(),
                role.getDescription(),
                role.isSystem(),
                permissions,
                role.getCreatedAt(),
                role.getVersion());
    }
}
