package com.example.KendyDigital.dto.ticket.request;

import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
        @NotNull TicketCategory category,
        @NotBlank String subject,
        @NotBlank String message,
        String orderCode,
        String depositCode,
        TicketPriority priority) {
}
