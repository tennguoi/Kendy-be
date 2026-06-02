package com.example.KendyDigital.dto;


import com.example.KendyDigital.model.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManualCreditBankTransactionRequest(
        @NotNull Long userId,
        String depositCode,
        @NotBlank String reason) {
}
