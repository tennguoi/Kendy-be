package com.example.KendyDigital.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorDisableRequest(
        @NotBlank String password,
        String code) {
}
