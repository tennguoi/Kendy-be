package com.example.KendyDigital.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExtendOrderRequest(
        @NotNull @Min(1) Integer minutes,
        @NotBlank String reason) {
}
