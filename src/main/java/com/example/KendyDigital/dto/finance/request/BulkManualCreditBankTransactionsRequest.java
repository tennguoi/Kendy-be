package com.example.KendyDigital.dto.finance.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BulkManualCreditBankTransactionsRequest(
        @NotEmpty List<Long> bankTransactionIds,
        @NotNull Long userId,
        @Size(max = 50) String depositCode,
        @NotBlank @Size(max = 500) String reason) {
}
