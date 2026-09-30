package com.example.KendyDigital.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthForgotPasswordRequest(
        @NotBlank @Email @Size(max = 100) String email) {
}
