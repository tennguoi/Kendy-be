package com.example.KendyDigital.dto.order.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record ReprocessOrderRequest(
        @NotBlank @Size(max = 500) String reason) {
}
