package com.example.KendyDigital.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record BulkRefundOrdersRequest(
        @NotEmpty List<String> orderCodes,
        @NotBlank String reason) {
}
