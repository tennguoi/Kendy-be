package com.example.KendyDigital.dto.order.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkRefundOrdersRequest(
        @NotEmpty List<String> orderCodes,
        @NotBlank String reason) {
}
