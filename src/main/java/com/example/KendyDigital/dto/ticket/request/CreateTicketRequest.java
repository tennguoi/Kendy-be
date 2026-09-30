package com.example.KendyDigital.dto.ticket.request;

import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotNull TicketCategory category,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 5000) String message,
        @Size(max = 50) String orderCode,
        @Size(max = 50) String depositCode,
        TicketPriority priority) {
}
