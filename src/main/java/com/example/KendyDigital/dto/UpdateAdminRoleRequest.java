package com.example.KendyDigital.dto;

import java.util.List;

public record UpdateAdminRoleRequest(
        String name,
        String description,
        List<Long> permissionIds) {
}
