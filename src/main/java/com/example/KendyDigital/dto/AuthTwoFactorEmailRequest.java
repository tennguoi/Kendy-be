package com.example.KendyDigital.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthTwoFactorEmailRequest(
        @Email @NotBlank String email,
        @NotBlank String password) {
}
