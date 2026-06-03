package com.example.KendyDigital.model;

import java.time.Instant;

import com.example.KendyDigital.common.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "tickets",
        indexes = {
                @Index(name = "idx_tickets_user_id", columnList = "user_id"),
                @Index(name = "idx_tickets_status", columnList = "status"),
                @Index(name = "idx_tickets_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tickets_code", columnNames = "ticket_code")
        })
public class Ticket extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_code", nullable = false)
    private String ticketCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderRecord order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_request_id")
    private DepositRequest depositRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketCategory category;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketStatus status = TicketStatus.PENDING_ADMIN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketPriority priority = TicketPriority.NORMAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private UserAccount assignedAdmin;

    @Column(name = "closed_at")
    private Instant closedAt;

    public Ticket(String ticketCode, UserAccount user, OrderRecord order, DepositRequest depositRequest,
            TicketCategory category, String subject, TicketPriority priority) {
        this.ticketCode = ticketCode;
        this.user = user;
        this.order = order;
        this.depositRequest = depositRequest;
        this.category = category;
        this.subject = subject;
        this.priority = priority == null ? TicketPriority.NORMAL : priority;
    }

    public void userReplied() {
        this.status = TicketStatus.PENDING_ADMIN;
    }

    public void adminReplied(UserAccount admin) {
        this.assignedAdmin = admin;
        this.status = TicketStatus.PENDING_USER;
    }

    public void updateAdminFields(TicketStatus status, TicketPriority priority, UserAccount assignedAdmin) {
        if (status != null) {
            this.status = status;
            if (status == TicketStatus.CLOSED || status == TicketStatus.RESOLVED) {
                this.closedAt = Instant.now();
            }
        }
        if (priority != null) {
            this.priority = priority;
        }
        if (assignedAdmin != null) {
            this.assignedAdmin = assignedAdmin;
        }
    }

    public void updatePriority(TicketPriority priority) {
        if (priority != null) {
            this.priority = priority;
        }
    }

    public void updateCategory(TicketCategory category) {
        if (category != null) {
            this.category = category;
        }
    }

    public void close() {
        this.status = TicketStatus.CLOSED;
        this.closedAt = Instant.now();
    }
}
