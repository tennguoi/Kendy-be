package com.example.KendyDigital.dto.deposit.request;

import jakarta.validation.constraints.NotBlank;

public record CancelDepositRequest(
        @NotBlank String reason) {
}
