package com.example.KendyDigital.service;
import com.example.KendyDigital.dto.ticket.request.AdminTicketUpdateRequest;
import com.example.KendyDigital.dto.ticket.request.CreateTicketMessageRequest;
import com.example.KendyDigital.dto.ticket.request.CreateTicketRequest;
import com.example.KendyDigital.dto.ticket.response.TicketAttachmentResponse;
import com.example.KendyDigital.dto.ticket.response.TicketMessageResponse;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;


import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.common.CodeGenerator;
import com.example.KendyDigital.model.*;
import com.example.KendyDigital.repository.*;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final CodeGenerator codeGenerator;
    private final AuditService auditService;
    private final AdminNotificationRepository adminNotificationRepository;
    private final FileStorageService fileStorageService;
    private final UserNotificationService userNotificationService;

    public TicketService(TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            TicketAttachmentRepository ticketAttachmentRepository,
            UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            DepositRequestRepository depositRequestRepository,
            CodeGenerator codeGenerator,
            AuditService auditService,
            AdminNotificationRepository adminNotificationRepository,
            FileStorageService fileStorageService,
            UserNotificationService userNotificationService) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
        this.adminNotificationRepository = adminNotificationRepository;
        this.fileStorageService = fileStorageService;
        this.userNotificationService = userNotificationService;
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
        adminNotificationRepository.save(new AdminNotification(null,
                "New ticket: " + ticket.getSubject(),
                "Ticket " + ticket.getTicketCode() + " created by " + user.getEmail()));
        return getForUser(userId, ticket.getTicketCode());
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForUser(Long userId, TicketStatus status) {
        return listForUser(userId, status, 0, 50);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForUser(Long userId, TicketStatus status, int page, int size) {
        List<Ticket> tickets = status == null
                ? ticketRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                : ticketRepository.findAllByUser_IdAndStatusOrderByCreatedAtDesc(userId, status, paged(page, size));
        return tickets
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> searchForUser(Long userId, String query, TicketStatus status, TicketCategory category,
            TicketPriority priority, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        return ticketRepository.searchUser(
                        userId,
                        likePattern(normalizedQuery),
                        parseLongOrNull(normalizedQuery),
                        status,
                        category,
                        priority,
                        paged(page, size))
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

    @Transactional
    public TicketResponse reopenForUser(Long userId, String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ensureOwner(ticket, userId);
        if (ticket.getStatus() != TicketStatus.CLOSED && ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only closed or resolved tickets can be reopened");
        }
        ticket.reopen();
        adminNotificationRepository.save(new AdminNotification(null,
                "Ticket reopened: " + ticket.getSubject(),
                "Ticket " + ticket.getTicketCode() + " reopened by user"));
        auditService.recordSystem("TICKET_REOPENED_BY_USER", "TICKET", ticket.getId(), "userId=" + userId);
        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForAdmin(TicketStatus status, Long userId) {
        return listForAdmin(status, userId, null);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listForAdmin(TicketStatus status, Long userId, Integer limit) {
        List<Ticket> tickets = userId != null
                ? ticketRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, page(limit))
                : status == null
                        ? ticketRepository.findAllByOrderByCreatedAtDesc(page(limit))
                        : ticketRepository.findAllByStatusOrderByCreatedAtDesc(status, page(limit));
        return tickets.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> searchForAdmin(String query, TicketStatus status, TicketCategory category,
            TicketPriority priority, Long userId, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        return ticketRepository.searchAdmin(
                        likePattern(normalizedQuery),
                        parseLongOrNull(normalizedQuery),
                        status,
                        category,
                        priority,
                        userId,
                        page(limit))
                .stream()
                .map(this::toResponse)
                .toList();
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
        userNotificationService.create(ticket.getUser().getId(),
                "Ticket replied: " + ticket.getSubject(),
                "Admin replied to ticket " + ticket.getTicketCode(),
                "TICKET",
                "/tickets/" + ticket.getTicketCode());
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
        userNotificationService.create(ticket.getUser().getId(),
                "Ticket updated: " + ticket.getSubject(),
                "Ticket " + ticket.getTicketCode() + " status is " + ticket.getStatus(),
                "TICKET",
                "/tickets/" + ticket.getTicketCode());
        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketAttachmentResponse> listAttachments(String ticketCode) {
        ensureTicketExists(ticketCode);
        return ticketAttachmentRepository.findAllByTicket_TicketCodeOrderByCreatedAtDesc(ticketCode)
                .stream()
                .map(TicketAttachmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketAttachmentResponse> listAttachmentsForUser(Long userId, String ticketCode) {
        Ticket ticket = ensureTicketExists(ticketCode);
        ensureOwner(ticket, userId);
        return ticketAttachmentRepository.findAllByTicket_TicketCodeOrderByCreatedAtDesc(ticketCode)
                .stream()
                .map(TicketAttachmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoredFile getAttachmentFileForUser(Long userId, String ticketCode, Long attachmentId) {
        Ticket ticket = ensureTicketExists(ticketCode);
        ensureOwner(ticket, userId);
        TicketAttachment attachment = requireAttachment(ticket, attachmentId);
        if (attachment.getStoredFile() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file not found");
        }
        return fileStorageService.getById(attachment.getStoredFile().getId());
    }

    @Transactional
    public void deleteAttachmentForUser(Long userId, String ticketCode, Long attachmentId) {
        Ticket ticket = ensureTicketExists(ticketCode);
        ensureOwner(ticket, userId);
        TicketAttachment attachment = requireAttachment(ticket, attachmentId);
        if (attachment.getStoredFile() == null || !userId.equals(attachment.getStoredFile().getUploadedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only your own attachments can be deleted");
        }
        fileStorageService.delete(attachment.getStoredFile().getId());
        ticketAttachmentRepository.delete(attachment);
        auditService.recordSystem("TICKET_ATTACHMENT_DELETED_BY_USER", "TICKET", ticket.getId(),
                "userId=" + userId + ",attachmentId=" + attachmentId);
    }

    @Transactional
    public void deleteAttachment(Long adminUserId, String ticketCode, Long attachmentId) {
        Ticket ticket = ensureTicketExists(ticketCode);
        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (!attachment.getTicket().getId().equals(ticket.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found");
        }
        if (attachment.getStoredFile() != null) {
            fileStorageService.delete(attachment.getStoredFile().getId());
        }
        ticketAttachmentRepository.delete(attachment);
        auditService.recordAdmin(adminUserId, "TICKET_ATTACHMENT_DELETED", "TICKET", ticket.getId(),
                "attachmentId=" + attachmentId);
    }

    @Transactional
    public TicketResponse updatePriority(Long adminUserId, String ticketCode, TicketPriority priority) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ticket.updatePriority(priority);
        auditService.recordAdmin(adminUserId, "TICKET_PRIORITY_UPDATED", "TICKET", ticket.getId(),
                "priority=" + priority);
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateCategory(Long adminUserId, String ticketCode, TicketCategory category) {
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ticket.updateCategory(category);
        auditService.recordAdmin(adminUserId, "TICKET_CATEGORY_UPDATED", "TICKET", ticket.getId(),
                "category=" + category);
        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listUnassigned(Integer limit) {
        return ticketRepository.findAllByAssignedAdminIsNullOrderByCreatedAtDesc(page(limit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listAssignedToMe(Long adminUserId, Integer limit) {
        return ticketRepository.findAllByAssignedAdmin_IdOrderByCreatedAtDesc(adminUserId, page(limit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TicketAttachmentResponse uploadAttachment(Long userId, String ticketCode, MultipartFile file) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ensureOwner(ticket, userId);
        ensureOpenForMessage(ticket);

        StoredFile storedFile = fileStorageService.store(file, userId);
        TicketAttachment attachment = ticketAttachmentRepository.save(
                new TicketAttachment(ticket, storedFile));
        return TicketAttachmentResponse.from(attachment);
    }

    @Transactional
    public TicketAttachmentResponse uploadAttachmentAdmin(Long adminUserId, String ticketCode, MultipartFile file) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ensureOpenForMessage(ticket);

        StoredFile storedFile = fileStorageService.store(file, adminUserId);
        TicketAttachment attachment = ticketAttachmentRepository.save(
                new TicketAttachment(ticket, storedFile));
        auditService.recordAdmin(adminUserId, "TICKET_ATTACHMENT_UPLOADED", "TICKET", ticket.getId(),
                "attachmentId=" + attachment.getId());
        return TicketAttachmentResponse.from(attachment);
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> resolutionTime() {
        List<Ticket> tickets = ticketRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1000));
        long closed = tickets.stream()
                .filter(ticket -> ticket.getClosedAt() != null)
                .count();
        double averageMinutes = tickets.stream()
                .filter(ticket -> ticket.getClosedAt() != null)
                .mapToLong(ticket -> java.time.Duration.between(ticket.getCreatedAt(), ticket.getClosedAt()).toMinutes())
                .average()
                .orElse(0);
        return java.util.Map.of(
                "closedTickets", closed,
                "averageResolutionMinutes", averageMinutes);
    }

    public byte[] previewBytes(StoredFile file) {
        return fileStorageService.previewBytes(file);
    }

    private TicketResponse toResponse(Ticket ticket) {
        List<TicketMessageResponse> messages = ticketMessageRepository
                .findByTicket_TicketCodeOrderByCreatedAtAsc(ticket.getTicketCode())
                .stream()
                .map(TicketMessageResponse::from)
                .toList();
        return TicketResponse.from(ticket, messages);
    }

    private Ticket ensureTicketExists(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private TicketAttachment requireAttachment(Ticket ticket, Long attachmentId) {
        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (!attachment.getTicket().getId().equals(ticket.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found");
        }
        return attachment;
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

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        return PageRequest.of(0, normalizedLimit);
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }
}
