package com.example.KendyDigital.dto.ticket.response;

import com.example.KendyDigital.model.ticket.TicketAttachment;
import java.time.Instant;

public record TicketAttachmentResponse(
        Long id,
        String ticketCode,
        String fileName,
        String contentType,
        Long sizeBytes,
        Long storedFileId,
        Instant createdAt) {
    public static TicketAttachmentResponse from(TicketAttachment attachment) {
        return new TicketAttachmentResponse(
                attachment.getId(),
                attachment.getTicket().getTicketCode(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getStoredFile() == null ? null : attachment.getStoredFile().getId(),
                attachment.getCreatedAt());
    }
}
