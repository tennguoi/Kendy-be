package com.example.KendyDigital.model.admin;

import com.example.KendyDigital.common.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "admin_notifications",
        indexes = {
                @Index(name = "idx_admin_notifications_user_read", columnList = "admin_user_id,read_at")
        })
public class AdminNotification extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "read_at")
    private java.time.Instant readAt;

    public AdminNotification(Long adminUserId, String title, String message) {
        this.adminUserId = adminUserId;
        this.title = title;
        this.message = message;
    }

    public void markRead() {
        this.readAt = java.time.Instant.now();
    }
}
