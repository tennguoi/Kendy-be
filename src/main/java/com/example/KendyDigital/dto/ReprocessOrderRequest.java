package com.example.KendyDigital.dto;

import jakarta.validation.constraints.NotBlank;

public record ReprocessOrderRequest(
        @NotBlank String reason) {
}
