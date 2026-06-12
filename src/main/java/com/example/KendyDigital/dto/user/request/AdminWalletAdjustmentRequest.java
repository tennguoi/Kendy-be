package com.example.KendyDigital.dto.user.request;

import com.example.KendyDigital.model.*;
import java.math.BigDecimal;

import com.example.KendyDigital.model.WalletTransactionDirection;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminWalletAdjustmentRequest(
        @NotNull WalletTransactionDirection direction,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String reason,
        String confirmationPassword) {
}
