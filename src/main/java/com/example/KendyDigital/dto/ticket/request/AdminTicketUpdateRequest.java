package com.example.KendyDigital.dto.ticket.request;

import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketStatus;

public record AdminTicketUpdateRequest(
        TicketStatus status,
        TicketPriority priority,
        Long assignedAdminId) {
}
