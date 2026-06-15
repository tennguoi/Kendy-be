package com.example.KendyDigital.dto.deposit.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateDepositRequest(
        @NotNull @DecimalMin(value = "1000.00", inclusive = false) BigDecimal amount) {
}
