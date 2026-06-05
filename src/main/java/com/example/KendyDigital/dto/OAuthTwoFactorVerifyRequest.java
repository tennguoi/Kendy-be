package com.example.KendyDigital.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthTwoFactorVerifyRequest(
        @NotBlank String challengeToken,
        @NotBlank String code) {
}
