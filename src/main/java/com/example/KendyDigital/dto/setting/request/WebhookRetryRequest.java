package com.example.KendyDigital.dto.setting.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WebhookRetryRequest(
        @NotNull UUID bankTransactionId,
        @Size(max = 50) String depositCode,
        @NotBlank @Size(max = 500) String reason) {
}
