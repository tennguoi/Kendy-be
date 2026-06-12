package com.example.KendyDigital.dto.order.request;

import jakarta.validation.constraints.NotBlank;

public record ReprocessOrderRequest(
        @NotBlank String reason) {
}
