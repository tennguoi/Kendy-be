package com.example.KendyDigital.model.ticket;

import com.example.KendyDigital.common.TimestampedEntity;
import com.example.KendyDigital.model.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ticket_messages",
        indexes = {
                @Index(name = "idx_ticket_messages_ticket_id", columnList = "ticket_id"),
                @Index(name = "idx_ticket_messages_created_at", columnList = "created_at")
        })
public class TicketMessage extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserAccount sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false, length = 32)
    private TicketSenderRole senderRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String attachments;

    public TicketMessage(Ticket ticket, UserAccount sender, TicketSenderRole senderRole, String message) {
        this.ticket = ticket;
        this.sender = sender;
        this.senderRole = senderRole;
        this.message = message;
    }

}
