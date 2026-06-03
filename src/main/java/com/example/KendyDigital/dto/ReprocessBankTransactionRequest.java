package com.example.KendyDigital.dto;

import jakarta.validation.constraints.NotBlank;

public record ReprocessBankTransactionRequest(
        String depositCode,
        @NotBlank String reason) {
}
