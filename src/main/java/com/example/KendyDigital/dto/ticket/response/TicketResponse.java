package com.example.KendyDigital.dto.ticket.response;

import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketStatus;
import java.time.Instant;
import java.util.List;

public record TicketResponse(
        Long id,
        String ticketCode,
        Long userId,
        Long orderId,
        Long depositRequestId,
        TicketCategory category,
        String subject,
        TicketStatus status,
        TicketPriority priority,
        Long assignedAdminId,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        List<TicketMessageResponse> messages,
        Long version) {

    public TicketResponse(Long id, String ticketCode, Long userId, Long orderId,
            Long depositRequestId, TicketCategory category, String subject,
            TicketStatus status, TicketPriority priority, Long assignedAdminId,
            Instant closedAt, Instant createdAt, Instant updatedAt,
            List<TicketMessageResponse> messages) {
        this(id, ticketCode, userId, orderId, depositRequestId, category, subject,
                status, priority, assignedAdminId, closedAt, createdAt, updatedAt,
                messages, 0L);
    }

    public static TicketResponse from(Ticket ticket, List<TicketMessageResponse> messages) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketCode(),
                ticket.getUser().getId(),
                ticket.getOrder() == null ? null : ticket.getOrder().getId(),
                ticket.getDepositRequest() == null ? null : ticket.getDepositRequest().getId(),
                ticket.getCategory(),
                ticket.getSubject(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignedAdmin() == null ? null : ticket.getAssignedAdmin().getId(),
                ticket.getClosedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                messages,
                ticket.getVersion());
    }
}
