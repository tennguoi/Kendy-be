package com.example.KendyDigital.dto.checkout.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateServiceCheckoutRequest(
        @NotNull Long serviceId,
        String inputData,
        @Size(max = 128) String idempotencyKey) {
}
