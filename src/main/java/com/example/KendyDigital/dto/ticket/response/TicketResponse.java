package com.example.KendyDigital.dto.ticket.response;

import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String ticketCode,
        UUID userId,
        UUID orderId,
        UUID depositRequestId,
        TicketCategory category,
        String subject,
        TicketStatus status,
        TicketPriority priority,
        UUID assignedAdminId,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        List<TicketMessageResponse> messages,
        Long version) {

    public TicketResponse(UUID id, String ticketCode, UUID userId, UUID orderId,
            UUID depositRequestId, TicketCategory category, String subject,
            TicketStatus status, TicketPriority priority, UUID assignedAdminId,
            Instant closedAt, Instant createdAt, Instant updatedAt,
            List<TicketMessageResponse> messages) {
        this(id, ticketCode, userId, orderId, depositRequestId, category, subject,
                status, priority, assignedAdminId, closedAt, createdAt, updatedAt,
                messages, 0L);
    }

    public static TicketResponse from(Ticket ticket, List<TicketMessageResponse> messages) {
        return new TicketResponse(
                ticket.getPublicId(),
                ticket.getTicketCode(),
                ticket.getUser().getPublicId(),
                ticket.getOrder() == null ? null : ticket.getOrder().getPublicId(),
                ticket.getDepositRequest() == null ? null : ticket.getDepositRequest().getPublicId(),
                ticket.getCategory(),
                ticket.getSubject(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignedAdmin() == null ? null : ticket.getAssignedAdmin().getPublicId(),
                ticket.getClosedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                messages,
                ticket.getVersion());
    }
}
