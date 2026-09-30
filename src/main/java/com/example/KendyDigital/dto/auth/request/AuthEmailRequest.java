package com.example.KendyDigital.dto.auth.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthEmailRequest(
        @NotBlank @Email @Size(max = 100) String email) {
}
