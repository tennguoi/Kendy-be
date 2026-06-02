package com.example.KendyDigital.dto;


import com.example.KendyDigital.model.*;
import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateDepositRequest(
        @NotNull @DecimalMin("10000.00") BigDecimal amount) {
}
