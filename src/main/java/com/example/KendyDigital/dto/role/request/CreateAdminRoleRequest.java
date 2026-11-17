package com.example.KendyDigital.dto.role.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record CreateAdminRoleRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 500) String description,
        List<UUID> permissionIds) {
}
