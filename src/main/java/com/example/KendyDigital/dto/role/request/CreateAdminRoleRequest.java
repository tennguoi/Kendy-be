package com.example.KendyDigital.dto.role.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateAdminRoleRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 500) String description,
        List<Long> permissionIds) {
}
