package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.ticket.request.AdminTicketUpdateRequest;
import com.example.KendyDigital.dto.ticket.request.CreateTicketMessageRequest;
import com.example.KendyDigital.dto.ticket.request.CreateTicketRequest;
import com.example.KendyDigital.dto.ticket.request.TicketFieldUpdateRequest;
import com.example.KendyDigital.dto.ticket.response.TicketAttachmentResponse;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.model.file.StoredFile;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketStatus;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.ticket.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/api/tickets")
    public TicketResponse create(Authentication authentication, @Valid @RequestBody CreateTicketRequest request) {
        return ticketService.create(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/tickets")
    public List<TicketResponse> list(Authentication authentication,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ticketService.listForUser(CurrentUser.require(authentication).userId(), status, page, size);
    }

    @GetMapping("/api/tickets/search")
    public List<TicketResponse> search(Authentication authentication,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ticketService.searchForUser(CurrentUser.require(authentication).userId(), query, status, category,
                priority, page, size);
    }

    @GetMapping("/api/tickets/{ticketCode}")
    public TicketResponse get(Authentication authentication, @PathVariable String ticketCode) {
        return ticketService.getForUser(CurrentUser.require(authentication).userId(), ticketCode);
    }

    @PostMapping("/api/tickets/{ticketCode}/messages")
    public TicketResponse addMessage(Authentication authentication, @PathVariable String ticketCode,
            @Valid @RequestBody CreateTicketMessageRequest request) {
        return ticketService.addUserMessage(CurrentUser.require(authentication).userId(), ticketCode, request);
    }

    @PostMapping("/api/tickets/{ticketCode}/close")
    public TicketResponse close(Authentication authentication, @PathVariable String ticketCode) {
        return ticketService.closeForUser(CurrentUser.require(authentication).userId(), ticketCode);
    }

    @PostMapping("/api/tickets/{ticketCode}/reopen")
    public TicketResponse reopen(Authentication authentication, @PathVariable String ticketCode) {
        return ticketService.reopenForUser(CurrentUser.require(authentication).userId(), ticketCode);
    }

    @GetMapping("/api/admin/tickets")
    public List<TicketResponse> listAdmin(@RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer limit) {
        return ticketService.listForAdmin(status, userId, limit);
    }

    @GetMapping("/api/admin/tickets/search")
    public List<TicketResponse> searchAdmin(@RequestParam(required = false) String query,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer limit) {
        return ticketService.searchForAdmin(query, status, category, priority, userId, limit);
    }

    @GetMapping("/api/admin/tickets/{ticketCode}")
    public TicketResponse getAdmin(@PathVariable String ticketCode) {
        return ticketService.getForAdmin(ticketCode);
    }

    @PostMapping(value = "/api/tickets/{ticketCode}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketAttachmentResponse uploadAttachment(Authentication authentication, @PathVariable String ticketCode,
            @RequestPart("file") MultipartFile file) {
        return ticketService.uploadAttachment(CurrentUser.require(authentication).userId(), ticketCode, file);
    }

    @GetMapping("/api/tickets/{ticketCode}/attachments")
    public List<TicketAttachmentResponse> listUserAttachments(Authentication authentication,
            @PathVariable String ticketCode) {
        return ticketService.listAttachmentsForUser(CurrentUser.require(authentication).userId(), ticketCode);
    }

    @GetMapping("/api/tickets/{ticketCode}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadUserAttachment(Authentication authentication, @PathVariable String ticketCode,
            @PathVariable Long attachmentId) {
        StoredFile file = ticketService.getAttachmentFileForUser(CurrentUser.require(authentication).userId(),
                ticketCode, attachmentId);
        return fileResponse(file, file.getContent());
    }

    @GetMapping("/api/tickets/{ticketCode}/attachments/{attachmentId}/preview")
    public ResponseEntity<byte[]> previewUserAttachment(Authentication authentication, @PathVariable String ticketCode,
            @PathVariable Long attachmentId) {
        StoredFile file = ticketService.getAttachmentFileForUser(CurrentUser.require(authentication).userId(),
                ticketCode, attachmentId);
        return fileResponse(file, ticketService.previewBytes(file));
    }

    @DeleteMapping("/api/tickets/{ticketCode}/attachments/{attachmentId}")
    public void deleteUserAttachment(Authentication authentication, @PathVariable String ticketCode,
            @PathVariable Long attachmentId) {
        ticketService.deleteAttachmentForUser(CurrentUser.require(authentication).userId(), ticketCode, attachmentId);
    }

    @GetMapping("/api/admin/tickets/{ticketCode}/attachments")
    public List<TicketAttachmentResponse> listAttachments(@PathVariable String ticketCode) {
        return ticketService.listAttachments(ticketCode);
    }

    @GetMapping("/api/admin/tickets/{ticketCode}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(Authentication authentication,
            @PathVariable String ticketCode, @PathVariable Long attachmentId) {
        StoredFile file = ticketService.getAttachmentFileForAdmin(ticketCode, attachmentId);
        return fileResponse(file, file.getContent());
    }

    @GetMapping("/api/admin/tickets/{ticketCode}/attachments/{attachmentId}/preview")
    public ResponseEntity<byte[]> previewAttachment(Authentication authentication,
            @PathVariable String ticketCode, @PathVariable Long attachmentId) {
        StoredFile file = ticketService.getAttachmentFileForAdmin(ticketCode, attachmentId);
        return fileResponse(file, ticketService.previewBytes(file));
    }

    @PostMapping(value = "/api/admin/tickets/{ticketCode}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketAttachmentResponse uploadAttachmentAdmin(Authentication authentication, @PathVariable String ticketCode,
            @RequestPart("file") MultipartFile file) {
        return ticketService.uploadAttachmentAdmin(CurrentUser.require(authentication).userId(), ticketCode, file);
    }

    @DeleteMapping("/api/admin/tickets/{ticketCode}/attachments/{attachmentId}")
    public void deleteAttachment(Authentication authentication, @PathVariable String ticketCode,
            @PathVariable Long attachmentId) {
        ticketService.deleteAttachment(CurrentUser.require(authentication).userId(), ticketCode, attachmentId);
    }

    @PatchMapping("/api/admin/tickets/{ticketCode}/priority")
    public TicketResponse updatePriority(Authentication authentication, @PathVariable String ticketCode,
            @Valid @RequestBody TicketFieldUpdateRequest request) {
        return ticketService.updatePriority(CurrentUser.require(authentication).userId(), ticketCode,
                request.priority());
    }

    @PatchMapping("/api/admin/tickets/{ticketCode}/category")
    public TicketResponse updateCategory(Authentication authentication, @PathVariable String ticketCode,
            @Valid @RequestBody TicketFieldUpdateRequest request) {
        return ticketService.updateCategory(CurrentUser.require(authentication).userId(), ticketCode,
                request.category());
    }

    @GetMapping("/api/admin/tickets/unassigned")
    public List<TicketResponse> listUnassigned(@RequestParam(required = false) Integer limit) {
        return ticketService.listUnassigned(limit);
    }

    @GetMapping("/api/admin/tickets/assigned-to-me")
    public List<TicketResponse> listAssignedToMe(Authentication authentication,
            @RequestParam(required = false) Integer limit) {
        return ticketService.listAssignedToMe(CurrentUser.require(authentication).userId(), limit);
    }

    @GetMapping("/api/admin/tickets/resolution-time")
    public Map<String, Object> resolutionTime() {
        return ticketService.resolutionTime();
    }

    @PostMapping("/api/admin/tickets/{ticketCode}/messages")
    public TicketResponse addAdminMessage(Authentication authentication, @PathVariable String ticketCode,
            @Valid @RequestBody CreateTicketMessageRequest request) {
        return ticketService.addAdminMessage(CurrentUser.require(authentication).userId(), ticketCode, request);
    }

    @PostMapping("/api/admin/tickets/{ticketCode}/update")
    public TicketResponse updateAdmin(Authentication authentication, @PathVariable String ticketCode,
            @Valid @RequestBody AdminTicketUpdateRequest request) {
        return ticketService.updateForAdmin(CurrentUser.require(authentication).userId(), ticketCode, request);
    }

    private ResponseEntity<byte[]> fileResponse(StoredFile file, byte[] content) {
        MediaType mediaType = file.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(file.getContentType());
        boolean isImage = file.getContentType() != null
                && file.getContentType().startsWith("image/");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        isImage
                                ? ContentDisposition.inline().filename(file.getFileName()).build().toString()
                                : ContentDisposition.attachment().filename(file.getFileName()).build().toString())
                .contentType(mediaType)
                .body(content);
    }
}
