package com.example.KendyDigital.dto.finance.request;

import jakarta.validation.constraints.NotBlank;

public record IgnoreBankTransactionRequest(
        @NotBlank String reason) {
}
