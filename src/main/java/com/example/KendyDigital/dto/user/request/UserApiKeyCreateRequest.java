package com.example.KendyDigital.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserApiKeyCreateRequest(
        @NotBlank @Size(max = 80) String name,
        List<String> scopes) {
}
