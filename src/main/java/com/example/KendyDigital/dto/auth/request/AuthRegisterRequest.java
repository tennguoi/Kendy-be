package com.example.KendyDigital.dto.auth.request;


import com.example.KendyDigital.model.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        String phone,
        @NotBlank @Size(min = 8) String password) {
}
