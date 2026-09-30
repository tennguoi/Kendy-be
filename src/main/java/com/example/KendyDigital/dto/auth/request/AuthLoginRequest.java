package com.example.KendyDigital.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthLoginRequest(
        @Email @NotBlank @Size(max = 100) String email,
        @NotBlank @Size(max = 64) String password,
        @Size(max = 10) String twoFactorCode) {
}
