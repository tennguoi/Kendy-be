package com.example.KendyDigital.dto;


import com.example.KendyDigital.model.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password) {
}
