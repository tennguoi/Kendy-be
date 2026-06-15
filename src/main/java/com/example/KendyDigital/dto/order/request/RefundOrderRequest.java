package com.example.KendyDigital.dto.order.request;

import jakarta.validation.constraints.NotBlank;

public record RefundOrderRequest(
        @NotBlank String reason) {
}
