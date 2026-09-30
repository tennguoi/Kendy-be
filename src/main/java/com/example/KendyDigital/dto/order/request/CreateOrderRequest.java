package com.example.KendyDigital.dto.order.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull Long serviceId,
        @Size(max = 10000) String inputData,
        @Size(max = 128) String idempotencyKey,
        @Size(max = 64) String couponCode) {
}
