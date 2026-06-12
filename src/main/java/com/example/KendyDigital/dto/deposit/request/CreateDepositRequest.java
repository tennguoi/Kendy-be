package com.example.KendyDigital.dto.deposit.request;


import com.example.KendyDigital.model.*;
import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateDepositRequest(
        @NotNull @DecimalMin(value = "1000.00", inclusive = false) BigDecimal amount) {
}
