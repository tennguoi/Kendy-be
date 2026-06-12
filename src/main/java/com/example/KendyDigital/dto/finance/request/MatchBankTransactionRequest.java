package com.example.KendyDigital.dto.finance.request;

import jakarta.validation.constraints.NotBlank;

public record MatchBankTransactionRequest(
        @NotBlank String depositCode,
        @NotBlank String reason) {
}
