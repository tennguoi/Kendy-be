package com.example.KendyDigital.dto.user.request;

import com.example.KendyDigital.model.wallet.WalletTransactionDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdminWalletAdjustmentRequest(
        @NotNull WalletTransactionDirection direction,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String reason,
        String confirmationPassword) {
}
