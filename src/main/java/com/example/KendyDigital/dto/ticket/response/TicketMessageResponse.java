package com.example.KendyDigital.dto.ticket.response;

import java.time.Instant;

import com.example.KendyDigital.model.TicketMessage;
import com.example.KendyDigital.model.TicketSenderRole;

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
