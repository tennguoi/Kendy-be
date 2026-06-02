package com.example.KendyDigital.dto;

import com.example.KendyDigital.model.TicketPriority;
import com.example.KendyDigital.model.TicketStatus;

public record AdminTicketUpdateRequest(
        TicketStatus status,
        TicketPriority priority,
        Long assignedAdminId) {
}
