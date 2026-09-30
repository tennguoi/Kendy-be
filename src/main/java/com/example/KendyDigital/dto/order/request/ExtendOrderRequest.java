package com.example.KendyDigital.dto.order.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExtendOrderRequest(
        @NotNull @Min(1) Integer minutes,
        @NotBlank @Size(max = 500) String reason) {
}
