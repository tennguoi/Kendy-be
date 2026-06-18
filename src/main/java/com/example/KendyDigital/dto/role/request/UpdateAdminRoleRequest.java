package com.example.KendyDigital.dto.role.request;

import java.util.List;
import jakarta.validation.constraints.Size;

public record UpdateAdminRoleRequest(
        @Size(max = 100) String name,
        @Size(max = 1000) String description,
        List<Long> permissionIds) {
}
