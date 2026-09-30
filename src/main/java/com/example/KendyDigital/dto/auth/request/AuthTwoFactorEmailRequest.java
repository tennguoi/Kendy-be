package com.example.KendyDigital.dto.auth.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthTwoFactorEmailRequest(
        @Email @NotBlank @Size(max = 100) String email,
        @NotBlank @Size(max = 64) String password) {
}
