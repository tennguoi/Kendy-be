package com.example.KendyDigital.dto.finance.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BulkManualCreditBankTransactionsRequest(
        @NotEmpty List<UUID> bankTransactionIds,
        @NotNull UUID userId,
        @Size(max = 50) String depositCode,
        @NotBlank @Size(max = 500) String reason) {
}
