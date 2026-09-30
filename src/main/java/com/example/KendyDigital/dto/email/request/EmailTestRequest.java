package com.example.KendyDigital.dto.email.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record EmailTestRequest(
        @NotBlank @Size(max = 100) String slug,
        @Email @Size(max = 100) String sendTo,
        Map<String, String> placeholders
) {}