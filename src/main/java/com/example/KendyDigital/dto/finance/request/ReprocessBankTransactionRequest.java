package com.example.KendyDigital.dto.finance.request;

import jakarta.validation.constraints.NotBlank;

public record ReprocessBankTransactionRequest(
        String depositCode,
        @NotBlank String reason) {
}
