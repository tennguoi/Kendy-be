package com.example.KendyDigital.dto.ticket.request;

import com.example.KendyDigital.model.TicketPriority;
import com.example.KendyDigital.model.TicketStatus;

public record AdminTicketUpdateRequest(
        TicketStatus status,
        TicketPriority priority,
        Long assignedAdminId) {
}
