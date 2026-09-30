package com.example.KendyDigital.dto.order.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @NotBlank @Size(max = 500) String reason) {
}
