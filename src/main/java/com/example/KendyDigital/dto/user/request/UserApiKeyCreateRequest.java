package com.example.KendyDigital.dto.user.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserApiKeyCreateRequest(
        @NotBlank @Size(max = 80) String name,
        List<String> scopes) {
}
