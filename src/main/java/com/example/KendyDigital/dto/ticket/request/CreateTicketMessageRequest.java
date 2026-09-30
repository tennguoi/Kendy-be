package com.example.KendyDigital.dto.ticket.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketMessageRequest(
        @NotBlank @Size(max = 5000) String message) {
}
