package com.example.KendyDigital.model.user;

import com.example.KendyDigital.common.TimestampedEntity;
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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_notifications",
        indexes = {
                @Index(name = "idx_user_notifications_user_id", columnList = "user_id"),
                @Index(name = "idx_user_notifications_read_at", columnList = "read_at")
        })
public class UserNotification extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 60)
    private String type;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "read_at")
    private Instant readAt;

    public UserNotification(UserAccount user, String title, String message, String type, String actionUrl) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.type = type;
        this.actionUrl = actionUrl;
    }

    public void markRead() {
        if (this.readAt == null) {
            this.readAt = Instant.now();
        }
    }
}
