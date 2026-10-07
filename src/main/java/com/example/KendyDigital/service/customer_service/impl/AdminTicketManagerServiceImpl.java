package com.example.KendyDigital.service.customer_service.impl;

import com.example.KendyDigital.dto.ticket.request.AdminTicketUpdateRequest;
import com.example.KendyDigital.dto.ticket.request.CreateTicketMessageRequest;
import com.example.KendyDigital.dto.ticket.response.TicketMessageResponse;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketMessage;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketSenderRole;
import com.example.KendyDigital.model.ticket.TicketStatus;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.repository.TicketMessageRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.customer_service.AdminTicketManagerService;
import com.example.KendyDigital.service.customer_service.helper.TicketNotificationHelper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminTicketManagerServiceImpl implements AdminTicketManagerService {
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final TicketNotificationHelper ticketNotificationHelper;

    public AdminTicketManagerServiceImpl(
            TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService,
            TicketNotificationHelper ticketNotificationHelper) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.ticketNotificationHelper = ticketNotificationHelper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listForAdmin(TicketStatus status, Long userId, Integer limit) {
        List<Ticket> tickets = userId != null
                ? ticketRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, page(limit))
                : status == null
                        ? ticketRepository.findAllByOrderByCreatedAtDesc(page(limit))
                        : ticketRepository.findAllByStatusOrderByCreatedAtDesc(status, page(limit));
        return toResponseList(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> searchForAdmin(String query, TicketStatus status, TicketCategory category,
            TicketPriority priority, Long userId, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        List<Ticket> tickets = ticketRepository.findAll(
                com.example.KendyDigital.repository.specification.TicketSpecifications.searchAdmin(
                        normalizedQuery, status, category, priority, userId),
                page(limit)).getContent();
        return toResponseList(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getForAdmin(String ticketCode) {
        return toResponse(ensureTicketExists(ticketCode));
    }

    @Override
    @Transactional
    public TicketResponse addAdminMessage(Long adminUserId, String ticketCode, CreateTicketMessageRequest request) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        UserAccount admin = userAccountRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        ticket.adminReplied(admin);
        ticketMessageRepository
                .save(new TicketMessage(ticket, admin, TicketSenderRole.ADMIN, request.message().trim()));
        auditService.recordAdmin(adminUserId, "TICKET_REPLIED", "TICKET", ticket.getId(), null);
        ticketNotificationHelper.notifyUserTicketReplied(ticket);
        return toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateForAdmin(Long adminUserId, String ticketCode, AdminTicketUpdateRequest request) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (request.version() != null && !request.version().equals(ticket.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CONCURRENT_ADMIN_CONFLICT");
        }
        UserAccount assignedAdmin = null;
        if (request.assignedAdminId() != null) {
            assignedAdmin = userAccountRepository.findById(request.assignedAdminId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned admin not found"));
            if (assignedAdmin.getRole() == UserRole.USER) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned user is not an admin");
            }
        }

        ticket.updateAdminFields(request.status(), request.priority(), assignedAdmin);
        auditService.recordAdmin(adminUserId, "TICKET_UPDATED", "TICKET", ticket.getId(),
                "status=" + request.status() + ",priority=" + request.priority());
        ticketNotificationHelper.notifyUserTicketUpdated(ticket);
        return toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updatePriority(Long adminUserId, String ticketCode, TicketPriority priority) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ticket.updatePriority(priority);
        auditService.recordAdmin(adminUserId, "TICKET_PRIORITY_UPDATED", "TICKET", ticket.getId(),
                "priority=" + priority);
        return toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateCategory(Long adminUserId, String ticketCode, TicketCategory category) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ticket.updateCategory(category);
        auditService.recordAdmin(adminUserId, "TICKET_CATEGORY_UPDATED", "TICKET", ticket.getId(),
                "category=" + category);
        return toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listUnassigned(Integer limit) {
        return toResponseList(
                ticketRepository.findAllByAssignedAdminIsNullOrderByCreatedAtDesc(page(limit)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listAssignedToMe(Long adminUserId, Integer limit) {
        return toResponseList(
                ticketRepository.findAllByAssignedAdmin_IdOrderByCreatedAtDesc(adminUserId, page(limit)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> resolutionTime() {
        List<Ticket> tickets = ticketRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1000));
        long closed = tickets.stream()
                .filter(ticket -> ticket.getClosedAt() != null)
                .count();
        double averageMinutes = tickets.stream()
                .filter(ticket -> ticket.getClosedAt() != null)
                .mapToLong(
                        ticket -> Duration.between(ticket.getCreatedAt(), ticket.getClosedAt()).toMinutes())
                .average()
                .orElse(0);
        return Map.of(
                "closedTickets", closed,
                "averageResolutionMinutes", averageMinutes);
    }

    private Ticket ensureTicketExists(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private TicketResponse toResponse(Ticket ticket) {
        List<TicketMessageResponse> messages = ticketMessageRepository
                .findByTicket_TicketCodeOrderByCreatedAtAsc(ticket.getTicketCode())
                .stream()
                .map(TicketMessageResponse::from)
                .toList();
        return TicketResponse.from(ticket, messages);
    }

    private List<TicketResponse> toResponseList(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return List.of();
        }
        List<String> codes = tickets.stream().map(Ticket::getTicketCode).toList();
        Map<String, List<TicketMessage>> messagesByTicket = ticketMessageRepository
                .findByTicket_TicketCodeInOrderByCreatedAtAsc(codes)
                .stream()
                .collect(Collectors.groupingBy(
                        msg -> msg.getTicket().getTicketCode(),
                        Collectors.toCollection(ArrayList::new)));
        return tickets.stream()
                .map(ticket -> {
                    List<TicketMessageResponse> messages = messagesByTicket
                            .getOrDefault(ticket.getTicketCode(), List.of())
                            .stream()
                            .map(TicketMessageResponse::from)
                            .toList();
                    return TicketResponse.from(ticket, messages);
                })
                .toList();
    }

    private String normalizeQuery(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        return PageRequest.of(0, normalizedLimit);
    }
}
