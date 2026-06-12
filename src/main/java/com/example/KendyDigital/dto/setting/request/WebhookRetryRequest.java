package com.example.KendyDigital.dto.setting.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WebhookRetryRequest(
        @NotNull Long bankTransactionId,
        String depositCode,
        @NotBlank String reason) {
}
