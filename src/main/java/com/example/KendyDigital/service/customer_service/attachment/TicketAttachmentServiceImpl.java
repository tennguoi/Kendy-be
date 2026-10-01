package com.example.KendyDigital.service.customer_service.attachment;

import com.example.KendyDigital.dto.ticket.response.TicketAttachmentResponse;
import com.example.KendyDigital.model.file.StoredFile;
import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.ticket.TicketAttachment;
import com.example.KendyDigital.model.ticket.TicketStatus;
import com.example.KendyDigital.repository.TicketAttachmentRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.file_integrations.FileStorageService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketAttachmentServiceImpl implements TicketAttachmentService {
    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    public TicketAttachmentServiceImpl(
            TicketRepository ticketRepository,
            TicketAttachmentRepository ticketAttachmentRepository,
            FileStorageService fileStorageService,
            AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketAttachmentResponse> listAttachments(String ticketCode) {
        ensureTicketExists(ticketCode);
        return ticketAttachmentRepository.findAllByTicket_TicketCodeOrderByCreatedAtDesc(ticketCode)
                .stream()
                .map(TicketAttachmentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketAttachmentResponse> listAttachmentsForUser(Long userId, String ticketCode) {
        Ticket ticket = ensureTicketExists(ticketCode);
        ensureOwner(ticket, userId);
        return ticketAttachmentRepository.findAllByTicket_TicketCodeOrderByCreatedAtDesc(ticketCode)
                .stream()
                .map(TicketAttachmentResponse::from)
                .toList();
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public StoredFile getAttachmentFileForAdmin(String ticketCode, Long attachmentId) {
        Ticket ticket = ensureTicketExists(ticketCode);
        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (!attachment.getTicket().getId().equals(ticket.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found");
        }
        if (attachment.getStoredFile() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file not found");
        }
        return fileStorageService.getById(attachment.getStoredFile().getId());
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public byte[] previewBytes(StoredFile file) {
        return fileStorageService.previewBytes(file);
    }

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

    private TicketAttachment requireAttachment(Ticket ticket, Long attachmentId) {
        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (!attachment.getTicket().getId().equals(ticket.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found");
        }
        return attachment;
    }
}
