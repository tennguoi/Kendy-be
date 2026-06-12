package com.example.KendyDigital.dto.order.request;


import com.example.KendyDigital.model.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull Long serviceId,
        String inputData,
        @Size(max = 128) String idempotencyKey) {
}
