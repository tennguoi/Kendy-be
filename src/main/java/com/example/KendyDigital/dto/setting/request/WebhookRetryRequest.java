package com.example.KendyDigital.dto.setting.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WebhookRetryRequest(
        @NotNull Long bankTransactionId,
        @Size(max = 50) String depositCode,
        @NotBlank @Size(max = 500) String reason) {
}
