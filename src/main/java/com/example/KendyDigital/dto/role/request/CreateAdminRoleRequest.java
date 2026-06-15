package com.example.KendyDigital.dto.role.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateAdminRoleRequest(
        @NotBlank String name,
        String description,
        List<Long> permissionIds) {
}
