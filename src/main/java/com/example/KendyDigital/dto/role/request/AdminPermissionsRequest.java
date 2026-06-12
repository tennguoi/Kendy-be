package com.example.KendyDigital.dto.role.request;

import java.util.List;

public record AdminPermissionsRequest(
        List<String> permissions,
        String reason) {
}
