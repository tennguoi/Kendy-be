package com.example.KendyDigital.dto.ticket.response;

import com.example.KendyDigital.model.ticket.TicketAttachment;
import java.time.Instant;
import java.util.UUID;

public record TicketAttachmentResponse(
        UUID id,
        String ticketCode,
        String fileName,
        String contentType,
        Long sizeBytes,
        UUID storedFileId,
        Instant createdAt) {
    public static TicketAttachmentResponse from(TicketAttachment attachment) {
        return new TicketAttachmentResponse(
                attachment.getPublicId(),
                attachment.getTicket().getTicketCode(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getStoredFile() == null ? null : attachment.getStoredFile().getPublicId(),
                attachment.getCreatedAt());
    }
}
