package com.example.KendyDigital.dto;

import java.util.List;

public record AdminPermissionsRequest(
        List<String> permissions,
        String reason) {
}
