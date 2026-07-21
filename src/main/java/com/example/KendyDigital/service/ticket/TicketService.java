package com.example.KendyDigital.service.ticket;

import com.example.KendyDigital.dto.ticket.request.AdminTicketUpdateRequest;
import com.example.KendyDigital.dto.ticket.request.CreateTicketMessageRequest;
import com.example.KendyDigital.dto.ticket.request.CreateTicketRequest;
import com.example.KendyDigital.dto.ticket.response.TicketAttachmentResponse;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.model.file.StoredFile;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketStatus;
import com.example.KendyDigital.repository.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface TicketService {
    TicketResponse create(Long userId, CreateTicketRequest request);

    List<TicketResponse> listForUser(Long userId, TicketStatus status);

    List<TicketResponse> listForUser(Long userId, TicketStatus status, int page, int size);

    List<TicketResponse> searchForUser(Long userId, String query, TicketStatus status, TicketCategory category,
            TicketPriority priority, int page, int size);

    TicketResponse getForUser(Long userId, String ticketCode);

    TicketResponse addUserMessage(Long userId, String ticketCode, CreateTicketMessageRequest request);

    TicketResponse closeForUser(Long userId, String ticketCode);

    TicketResponse reopenForUser(Long userId, String ticketCode);

    List<TicketResponse> listForAdmin(TicketStatus status, Long userId);

    List<TicketResponse> listForAdmin(TicketStatus status, Long userId, Integer limit);

    List<TicketResponse> searchForAdmin(String query, TicketStatus status, TicketCategory category,
            TicketPriority priority, Long userId, Integer limit);

    TicketResponse getForAdmin(String ticketCode);

    TicketResponse addAdminMessage(Long adminUserId, String ticketCode, CreateTicketMessageRequest request);

    TicketResponse updateForAdmin(Long adminUserId, String ticketCode, AdminTicketUpdateRequest request);

    List<TicketAttachmentResponse> listAttachments(String ticketCode);

    List<TicketAttachmentResponse> listAttachmentsForUser(Long userId, String ticketCode);

    StoredFile getAttachmentFileForUser(Long userId, String ticketCode, Long attachmentId);

    StoredFile getAttachmentFileForAdmin(String ticketCode, Long attachmentId);

    void deleteAttachmentForUser(Long userId, String ticketCode, Long attachmentId);

    void deleteAttachment(Long adminUserId, String ticketCode, Long attachmentId);

    TicketResponse updatePriority(Long adminUserId, String ticketCode, TicketPriority priority);

    TicketResponse updateCategory(Long adminUserId, String ticketCode, TicketCategory category);

    List<TicketResponse> listUnassigned(Integer limit);

    List<TicketResponse> listAssignedToMe(Long adminUserId, Integer limit);

    TicketAttachmentResponse uploadAttachment(Long userId, String ticketCode, MultipartFile file);

    TicketAttachmentResponse uploadAttachmentAdmin(Long adminUserId, String ticketCode, MultipartFile file);

    java.util.Map<String, Object> resolutionTime();

    byte[] previewBytes(StoredFile file);
}
