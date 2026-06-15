package com.example.KendyDigital.dto.ticket.request;

import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;

public record TicketFieldUpdateRequest(
        TicketPriority priority,
        TicketCategory category) {
}
