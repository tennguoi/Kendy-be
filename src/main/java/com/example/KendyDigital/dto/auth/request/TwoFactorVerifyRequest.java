package com.example.KendyDigital.dto.auth.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record TwoFactorVerifyRequest(
        @NotBlank @Size(max = 10) String code) {
}
