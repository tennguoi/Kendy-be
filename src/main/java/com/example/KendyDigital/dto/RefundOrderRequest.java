package com.example.KendyDigital.dto;


import com.example.KendyDigital.model.*;
import jakarta.validation.constraints.NotBlank;

public record RefundOrderRequest(
        @NotBlank String reason) {
}
