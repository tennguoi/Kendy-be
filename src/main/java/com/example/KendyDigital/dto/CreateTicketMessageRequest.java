package com.example.KendyDigital.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTicketMessageRequest(
        @NotBlank String message) {
}
