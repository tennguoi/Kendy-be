package com.example.KendyDigital.service.customer_service.impl;

import com.example.KendyDigital.common.CodeGenerator;
import com.example.KendyDigital.dto.ticket.request.AdminTicketUpdateRequest;
import com.example.KendyDigital.dto.ticket.request.CreateTicketMessageRequest;
import com.example.KendyDigital.dto.ticket.request.CreateTicketRequest;
import com.example.KendyDigital.dto.ticket.response.TicketAttachmentResponse;
import com.example.KendyDigital.dto.ticket.response.TicketMessageResponse;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.model.deposit.DepositRequest;
import com.example.KendyDigital.model.file.StoredFile;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketMessage;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketSenderRole;
import com.example.KendyDigital.model.ticket.TicketStatus;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketMessageRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.customer_service.AdminTicketManagerService;
import com.example.KendyDigital.service.customer_service.TicketService;
import com.example.KendyDigital.service.customer_service.attachment.TicketAttachmentService;
import com.example.KendyDigital.service.customer_service.helper.TicketNotificationHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final CodeGenerator codeGenerator;
    private final AuditService auditService;
    private final TicketNotificationHelper ticketNotificationHelper;
    private final TicketAttachmentService ticketAttachmentService;
    private final AdminTicketManagerService adminTicketManagerService;

    public TicketServiceImpl(
            TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            DepositRequestRepository depositRequestRepository,
            CodeGenerator codeGenerator,
            AuditService auditService,
            TicketNotificationHelper ticketNotificationHelper,
            TicketAttachmentService ticketAttachmentService,
            AdminTicketManagerService adminTicketManagerService) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
        this.ticketNotificationHelper = ticketNotificationHelper;
        this.ticketAttachmentService = ticketAttachmentService;
        this.adminTicketManagerService = adminTicketManagerService;
    }

    @Override
    @Transactional
    public TicketResponse create(Long userId, CreateTicketRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        OrderRecord order = resolveOrder(userId, request.orderCode());
        DepositRequest deposit = resolveDeposit(userId, request.depositCode());

        Ticket ticket = ticketRepository.save(new Ticket(
                nextTicketCode(),
                user,
                order,
                deposit,
                request.category(),
                request.subject().trim(),
                request.priority()));
        ticketMessageRepository.save(new TicketMessage(
                ticket,
                user,
                TicketSenderRole.USER,
                request.message().trim()));

        auditService.recordSystem("TICKET_CREATED", "TICKET", ticket.getId(), "userId=" + userId);
        ticketNotificationHelper.notifyNewTicket(ticket, user);
        return getForUser(userId, ticket.getTicketCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listForUser(Long userId, TicketStatus status) {
        return listForUser(userId, status, 0, 50);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listForUser(Long userId, TicketStatus status, int page, int size) {
        List<Ticket> tickets = status == null
                ? ticketRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                : ticketRepository.findAllByUser_IdAndStatusOrderByCreatedAtDesc(userId, status, paged(page, size));
        return toResponseList(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> searchForUser(Long userId, String query, TicketStatus status, TicketCategory category,
            TicketPriority priority, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        String pattern = likePattern(normalizedQuery);
        Long queryId = parseLongOrNull(normalizedQuery);
        List<Ticket> tickets = ticketRepository.searchUser(userId, pattern, queryId, status, category, priority,
                paged(page, size));
        return toResponseList(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getForUser(Long userId, String ticketCode) {
        Ticket ticket = ensureTicketExists(ticketCode);
        ensureOwner(ticket, userId);
        return toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse addUserMessage(Long userId, String ticketCode, CreateTicketMessageRequest request) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ensureOwner(ticket, userId);
        ensureOpenForMessage(ticket);

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ticket.userReplied();
        ticketMessageRepository.save(new TicketMessage(
                ticket,
                user,
                TicketSenderRole.USER,
                request.message().trim()));
        auditService.recordSystem("TICKET_USER_REPLIED", "TICKET", ticket.getId(), "userId=" + userId);
        return toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse closeForUser(Long userId, String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ensureOwner(ticket, userId);
        ticket.close();
        auditService.recordSystem("TICKET_CLOSED_BY_USER", "TICKET", ticket.getId(), "userId=" + userId);
        return toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse reopenForUser(Long userId, String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ensureOwner(ticket, userId);
        if (ticket.getStatus() != TicketStatus.CLOSED && ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only closed or resolved tickets can be reopened");
        }
        ticket.reopen();
        ticketNotificationHelper.notifyTicketReopened(ticket);
        auditService.recordSystem("TICKET_REOPENED_BY_USER", "TICKET", ticket.getId(), "userId=" + userId);
        return toResponse(ticket);
    }

    // --- Admin Operations Delegated to AdminTicketManagerService ---

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listForAdmin(TicketStatus status, Long userId) {
        return adminTicketManagerService.listForAdmin(status, userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listForAdmin(TicketStatus status, Long userId, Integer limit) {
        return adminTicketManagerService.listForAdmin(status, userId, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> searchForAdmin(String query, TicketStatus status, TicketCategory category,
            TicketPriority priority, Long userId, Integer limit) {
        return adminTicketManagerService.searchForAdmin(query, status, category, priority, userId, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getForAdmin(String ticketCode) {
        return adminTicketManagerService.getForAdmin(ticketCode);
    }

    @Override
    @Transactional
    public TicketResponse addAdminMessage(Long adminUserId, String ticketCode, CreateTicketMessageRequest request) {
        return adminTicketManagerService.addAdminMessage(adminUserId, ticketCode, request);
    }

    @Override
    @Transactional
    public TicketResponse updateForAdmin(Long adminUserId, String ticketCode, AdminTicketUpdateRequest request) {
        return adminTicketManagerService.updateForAdmin(adminUserId, ticketCode, request);
    }

    @Override
    @Transactional
    public TicketResponse updatePriority(Long adminUserId, String ticketCode, TicketPriority priority) {
        return adminTicketManagerService.updatePriority(adminUserId, ticketCode, priority);
    }

    @Override
    @Transactional
    public TicketResponse updateCategory(Long adminUserId, String ticketCode, TicketCategory category) {
        return adminTicketManagerService.updateCategory(adminUserId, ticketCode, category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listUnassigned(Integer limit) {
        return adminTicketManagerService.listUnassigned(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listAssignedToMe(Long adminUserId, Integer limit) {
        return adminTicketManagerService.listAssignedToMe(adminUserId, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> resolutionTime() {
        return adminTicketManagerService.resolutionTime();
    }

    // --- Attachment Operations Delegated to TicketAttachmentService ---

    @Override
    @Transactional(readOnly = true)
    public List<TicketAttachmentResponse> listAttachments(String ticketCode) {
        return ticketAttachmentService.listAttachments(ticketCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketAttachmentResponse> listAttachmentsForUser(Long userId, String ticketCode) {
        return ticketAttachmentService.listAttachmentsForUser(userId, ticketCode);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile getAttachmentFileForUser(Long userId, String ticketCode, Long attachmentId) {
        return ticketAttachmentService.getAttachmentFileForUser(userId, ticketCode, attachmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile getAttachmentFileForAdmin(String ticketCode, Long attachmentId) {
        return ticketAttachmentService.getAttachmentFileForAdmin(ticketCode, attachmentId);
    }

    @Override
    @Transactional
    public void deleteAttachmentForUser(Long userId, String ticketCode, Long attachmentId) {
        ticketAttachmentService.deleteAttachmentForUser(userId, ticketCode, attachmentId);
    }

    @Override
    @Transactional
    public void deleteAttachment(Long adminUserId, String ticketCode, Long attachmentId) {
        ticketAttachmentService.deleteAttachment(adminUserId, ticketCode, attachmentId);
    }

    @Override
    @Transactional
    public TicketAttachmentResponse uploadAttachment(Long userId, String ticketCode, MultipartFile file) {
        return ticketAttachmentService.uploadAttachment(userId, ticketCode, file);
    }

    @Override
    @Transactional
    public TicketAttachmentResponse uploadAttachmentAdmin(Long adminUserId, String ticketCode, MultipartFile file) {
        return ticketAttachmentService.uploadAttachmentAdmin(adminUserId, ticketCode, file);
    }

    @Override
    public byte[] previewBytes(StoredFile file) {
        return ticketAttachmentService.previewBytes(file);
    }

    // --- Helper Methods ---

    private Ticket ensureTicketExists(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
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

    private String nextTicketCode() {
        String code;
        do {
            code = codeGenerator.generate("TK", 10);
        } while (ticketRepository.existsByTicketCode(code));
        return code;
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

    private String likePattern(String value) {
        return value == null ? null : "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    private Long parseLongOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }
}
