package com.example.KendyDigital.dto.ticket.request;

import com.example.KendyDigital.model.TicketCategory;
import com.example.KendyDigital.model.TicketPriority;

public record TicketFieldUpdateRequest(
        TicketPriority priority,
        TicketCategory category) {
}
