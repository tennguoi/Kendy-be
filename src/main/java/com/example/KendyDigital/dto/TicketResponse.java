package com.example.KendyDigital.dto;

import java.time.Instant;
import java.util.List;

import com.example.KendyDigital.model.Ticket;
import com.example.KendyDigital.model.TicketCategory;
import com.example.KendyDigital.model.TicketPriority;
import com.example.KendyDigital.model.TicketStatus;

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
        List<TicketMessageResponse> messages) {
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
                messages);
    }
}
