package com.example.KendyDigital.dto.email.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record EmailTestRequest(
        @NotBlank String slug,
        @Email String sendTo,
        Map<String, String> placeholders
) {}