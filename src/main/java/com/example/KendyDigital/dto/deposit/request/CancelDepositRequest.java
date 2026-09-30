package com.example.KendyDigital.dto.deposit.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record CancelDepositRequest(
        @NotBlank @Size(max = 500) String reason) {
}
