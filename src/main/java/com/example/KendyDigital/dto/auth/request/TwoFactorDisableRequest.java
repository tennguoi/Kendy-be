package com.example.KendyDigital.dto.auth.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record TwoFactorDisableRequest(
        @NotBlank @Size(max = 64) String password,
        @Size(max = 10) String code) {
}
