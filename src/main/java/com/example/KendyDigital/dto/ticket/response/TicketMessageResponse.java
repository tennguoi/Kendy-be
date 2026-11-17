package com.example.KendyDigital.dto.ticket.response;

import com.example.KendyDigital.model.ticket.TicketMessage;
import com.example.KendyDigital.model.ticket.TicketSenderRole;
import java.time.Instant;
import java.util.UUID;

public record TicketMessageResponse(
        UUID id,
        UUID senderId,
        TicketSenderRole senderRole,
        String message,
        Instant createdAt) {
    public static TicketMessageResponse from(TicketMessage message) {
        return new TicketMessageResponse(
                message.getPublicId(),
                message.getSender() == null ? null : message.getSender().getPublicId(),
                message.getSenderRole(),
                message.getMessage(),
                message.getCreatedAt());
    }
}
