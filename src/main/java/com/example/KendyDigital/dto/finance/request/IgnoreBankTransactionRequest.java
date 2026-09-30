package com.example.KendyDigital.dto.finance.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record IgnoreBankTransactionRequest(
        @NotBlank @Size(max = 500) String reason) {
}
