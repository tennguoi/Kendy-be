package com.example.KendyDigital.dto.auth.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record AuthVerifyEmailRequest(
        @NotBlank @Size(max = 128) String token) {
}
