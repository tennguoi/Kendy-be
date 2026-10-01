package com.example.KendyDigital.service.customer_service.attachment;

import com.example.KendyDigital.dto.ticket.response.TicketAttachmentResponse;
import com.example.KendyDigital.model.file.StoredFile;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface TicketAttachmentService {
    List<TicketAttachmentResponse> listAttachments(String ticketCode);
    List<TicketAttachmentResponse> listAttachmentsForUser(Long userId, String ticketCode);
    StoredFile getAttachmentFileForUser(Long userId, String ticketCode, Long attachmentId);
    StoredFile getAttachmentFileForAdmin(String ticketCode, Long attachmentId);
    TicketAttachmentResponse uploadAttachment(Long userId, String ticketCode, MultipartFile file);
    TicketAttachmentResponse uploadAttachmentAdmin(Long adminUserId, String ticketCode, MultipartFile file);
    void deleteAttachmentForUser(Long userId, String ticketCode, Long attachmentId);
    void deleteAttachment(Long adminUserId, String ticketCode, Long attachmentId);
    byte[] previewBytes(StoredFile file);
}
