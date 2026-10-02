package com.example.KendyDigital.dto.role.request;

import java.util.List;
import jakarta.validation.constraints.Size;

public record UpdateAdminRoleRequest(
        @Size(max = 100) String name,
        @Size(max = 1000) String description,
        List<Long> permissionIds,
        Long version) {

    public UpdateAdminRoleRequest(String name, String description, List<Long> permissionIds) {
        this(name, description, permissionIds, null);
    }
}

