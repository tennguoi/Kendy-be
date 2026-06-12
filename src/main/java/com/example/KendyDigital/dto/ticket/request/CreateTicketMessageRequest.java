package com.example.KendyDigital.dto.ticket.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTicketMessageRequest(
        @NotBlank String message) {
}
