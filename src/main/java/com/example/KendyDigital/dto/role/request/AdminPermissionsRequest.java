package com.example.KendyDigital.dto.role.request;


import jakarta.validation.constraints.Size;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record AdminPermissionsRequest(
        @NotEmpty List<@NotBlank String> permissions,
        @Size(max = 500) String reason) {
}
