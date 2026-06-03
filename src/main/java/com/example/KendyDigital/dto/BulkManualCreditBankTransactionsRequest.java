package com.example.KendyDigital.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BulkManualCreditBankTransactionsRequest(
        @NotEmpty List<Long> bankTransactionIds,
        @NotNull Long userId,
        String depositCode,
        @NotBlank String reason) {
}
