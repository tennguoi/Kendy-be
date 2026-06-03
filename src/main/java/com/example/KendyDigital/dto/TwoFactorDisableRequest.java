package com.example.KendyDigital.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorDisableRequest(
        @NotBlank String password,
        String code) {
}
