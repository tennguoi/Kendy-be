package com.example.KendyDigital.dto.role.response;

import java.time.Instant;

import com.example.KendyDigital.model.AdminPermission;

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
