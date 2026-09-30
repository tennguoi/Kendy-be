package com.example.KendyDigital.dto.order.request;


import jakarta.validation.constraints.Size;
public record AdminOrderUpdateRequest(
        @Size(max = 20000) String resultData,
        @Size(max = 2000) String adminNote) {
}
