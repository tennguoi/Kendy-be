package com.example.KendyDigital.dto;

import java.time.Instant;

import com.example.KendyDigital.model.TicketAttachment;

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
