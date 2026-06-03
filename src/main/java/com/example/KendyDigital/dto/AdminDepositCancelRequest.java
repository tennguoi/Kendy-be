package com.example.KendyDigital.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminDepositCancelRequest(
        @NotBlank String reason) {
}
