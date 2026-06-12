package com.example.KendyDigital.dto.role.request;

import java.util.List;

public record UpdateAdminRoleRequest(
        String name,
        String description,
        List<Long> permissionIds) {
}
