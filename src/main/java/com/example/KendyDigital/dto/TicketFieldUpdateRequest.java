package com.example.KendyDigital.dto;

import com.example.KendyDigital.model.TicketCategory;
import com.example.KendyDigital.model.TicketPriority;

public record TicketFieldUpdateRequest(
        TicketPriority priority,
        TicketCategory category) {
}
