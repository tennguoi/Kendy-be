package com.example.KendyDigital.dto.monitoring.request;

import jakarta.validation.constraints.Size;

public record FreezeWalletRequest(
        @Size(max = 255) String reason) {
}
