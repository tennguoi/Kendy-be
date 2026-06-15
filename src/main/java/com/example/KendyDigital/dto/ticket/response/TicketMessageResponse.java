package com.example.KendyDigital.dto.ticket.response;

import com.example.KendyDigital.model.ticket.TicketMessage;
import com.example.KendyDigital.model.ticket.TicketSenderRole;
import java.time.Instant;

public record TicketMessageResponse(
        Long id,
        Long senderId,
        TicketSenderRole senderRole,
        String message,
        Instant createdAt) {
    public static TicketMessageResponse from(TicketMessage message) {
        return new TicketMessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSenderRole(),
                message.getMessage(),
                message.getCreatedAt());
    }
}
