package com.example.KendyDigital.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorVerifyRequest(
        @NotBlank String code) {
}
