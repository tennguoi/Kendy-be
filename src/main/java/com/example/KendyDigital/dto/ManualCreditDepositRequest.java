package com.example.KendyDigital.dto;

import jakarta.validation.constraints.NotBlank;

public record ManualCreditDepositRequest(
        @NotBlank String reason) {
}
