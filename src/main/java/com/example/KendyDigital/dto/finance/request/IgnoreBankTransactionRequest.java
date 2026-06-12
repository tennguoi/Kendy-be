package com.example.KendyDigital.dto.finance.request;


import com.example.KendyDigital.model.*;
import jakarta.validation.constraints.NotBlank;

public record IgnoreBankTransactionRequest(
        @NotBlank String reason) {
}
