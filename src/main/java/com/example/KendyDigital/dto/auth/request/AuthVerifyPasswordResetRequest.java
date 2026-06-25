package com.example.KendyDigital.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

public record AuthVerifyPasswordResetRequest(
        @NotBlank String token) {
}
