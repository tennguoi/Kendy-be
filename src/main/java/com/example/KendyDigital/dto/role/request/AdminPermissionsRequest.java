package com.example.KendyDigital.dto.role.request;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record AdminPermissionsRequest(
        @NotEmpty List<@NotBlank String> permissions,
        String reason) {
}
