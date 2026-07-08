package com.example.KendyDigital.dto.order.request;

import jakarta.validation.constraints.NotNull;

public record ManualOrderTaskStatusRequest(
        @NotNull Boolean completed) {
}
