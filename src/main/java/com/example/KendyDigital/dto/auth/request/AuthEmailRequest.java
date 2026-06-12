package com.example.KendyDigital.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthEmailRequest(
        @NotBlank @Email String email) {
}
