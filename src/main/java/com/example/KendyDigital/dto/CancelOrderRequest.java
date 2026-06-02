package com.example.KendyDigital.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(
        @NotBlank String reason) {
}
