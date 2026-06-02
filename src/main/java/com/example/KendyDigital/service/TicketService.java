package com.example.KendyDigital.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.common.CodeGenerator;
import com.example.KendyDigital.dto.*;
import com.example.KendyDigital.model.*;
import com.example.KendyDigital.repository.*;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final CodeGenerator codeGenerator;
    private final AuditService auditService;

    public TicketService(TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            DepositRequestRepository depositRequestRepository,
            CodeGenerator codeGenerator,
            AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
    }

    @Transactional
    public TicketResponse create(Long userId, CreateTicketRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        OrderRecord order = resolveOrder(userId, request.orderCode());
        DepositRequest depositRequest = resolveDeposit(userId, request.depositCode());

        Ticket ticket = ticketRepository.save(new Ticket(
                nextTicketCode(),
                user,
                order,
                depositRequest,
                request.category(),
                request.subject().trim(),
                request.priority()));
        ticketMessageRepository.save(new TicketMessage(
                ticket,
                user,
                TicketSenderRole.USER,
                request.message().trim()));

        auditService.recordSystem("TICKET_CREATED", "TICKET", ticket.getId(), "userId=" + userId);
        return getForUser(userId, ticket.getTicketCode());
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForUser(Long userId, TicketStatus status) {
        List<Ticket> tickets = status == null
                ? ticketRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))
                : ticketRepository.findAllByUser_IdAndStatusOrderByCreatedAtDesc(userId, status, PageRequest.of(0, 50));
        return tickets
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getForUser(Long userId, String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ensureOwner(ticket, userId);
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse addUserMessage(Long userId, String ticketCode, CreateTicketMessageRequest request) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ensureOwner(ticket, userId);
        ensureOpenForMessage(ticket);

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ticket.userReplied();
        ticketMessageRepository.save(new TicketMessage(ticket, user, TicketSenderRole.USER, request.message().trim()));
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse closeForUser(Long userId, String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ensureOwner(ticket, userId);
        ticket.close();
        auditService.recordSystem("TICKET_CLOSED_BY_USER", "TICKET", ticket.getId(), "userId=" + userId);
        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForAdmin(TicketStatus status, Long userId) {
        List<Ticket> tickets = userId != null
                ? ticketRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100))
                : status == null
                        ? ticketRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100))
                        : ticketRepository.findAllByStatusOrderByCreatedAtDesc(status, PageRequest.of(0, 100));
        return tickets.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getForAdmin(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse addAdminMessage(Long adminUserId, String ticketCode, CreateTicketMessageRequest request) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ensureOpenForMessage(ticket);

        UserAccount admin = userAccountRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        ticket.adminReplied(admin);
        ticketMessageRepository.save(new TicketMessage(ticket, admin, TicketSenderRole.ADMIN, request.message().trim()));
        auditService.recordAdmin(adminUserId, "TICKET_REPLIED", "TICKET", ticket.getId(), null);
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateForAdmin(Long adminUserId, String ticketCode, AdminTicketUpdateRequest request) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
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
        return toResponse(ticket);
    }

    private TicketResponse toResponse(Ticket ticket) {
        List<TicketMessageResponse> messages = ticketMessageRepository
                .findByTicket_TicketCodeOrderByCreatedAtAsc(ticket.getTicketCode())
                .stream()
                .map(TicketMessageResponse::from)
                .toList();
        return TicketResponse.from(ticket, messages);
    }

    private OrderRecord resolveOrder(Long userId, String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            return null;
        }
        OrderRecord order = orderRepository.findByOrderCode(orderCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return order;
    }

    private DepositRequest resolveDeposit(Long userId, String depositCode) {
        if (depositCode == null || depositCode.isBlank()) {
            return null;
        }
        DepositRequest deposit = depositRequestRepository.findByDepositCode(depositCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit not found"));
        if (!deposit.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit not found");
        }
        return deposit;
    }

    private void ensureOwner(Ticket ticket, Long userId) {
        if (!ticket.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
        }
    }

    private void ensureOpenForMessage(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket is already closed");
        }
    }

    private String nextTicketCode() {
        String code;
        do {
            code = codeGenerator.generate("TK", 10);
        } while (ticketRepository.existsByTicketCode(code));
        return code;
    }
}
