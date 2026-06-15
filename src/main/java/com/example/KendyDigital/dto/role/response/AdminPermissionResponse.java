package com.example.KendyDigital.dto.role.response;

import com.example.KendyDigital.model.admin.AdminPermission;
import java.time.Instant;

public record AdminPermissionResponse(
        Long id,
        String code,
        String description,
        String module,
        Instant createdAt) {
    public static AdminPermissionResponse from(AdminPermission permission) {
        return new AdminPermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getDescription(),
                permission.getModule(),
                permission.getCreatedAt());
    }
}
