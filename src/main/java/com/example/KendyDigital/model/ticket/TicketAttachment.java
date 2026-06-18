package com.example.KendyDigital.model.ticket;

import com.example.KendyDigital.common.TimestampedEntity;
import com.example.KendyDigital.model.file.StoredFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ticket_attachments",
        indexes = {
                @Index(name = "idx_ticket_attachments_ticket_id", columnList = "ticket_id")
        })
public class TicketAttachment extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id")
    private StoredFile storedFile;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    public TicketAttachment(Ticket ticket, String fileName, String contentType, Long sizeBytes) {
        this.ticket = ticket;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public TicketAttachment(Ticket ticket, StoredFile storedFile) {
        this.ticket = ticket;
        this.storedFile = storedFile;
        this.fileName = storedFile.getFileName();
        this.contentType = storedFile.getContentType();
        this.sizeBytes = storedFile.getSizeBytes();
    }
}
