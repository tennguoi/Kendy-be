package com.example.KendyDigital.model.user;

import com.example.KendyDigital.common.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_notification_settings")
public class UserNotificationSettings extends TimestampedEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "order_updates", nullable = false)
    private boolean orderUpdates = true;

    @Column(name = "deposit_updates", nullable = false)
    private boolean depositUpdates = true;

    @Column(name = "ticket_updates", nullable = false)
    private boolean ticketUpdates = true;

    @Column(name = "wallet_updates", nullable = false)
    private boolean walletUpdates = true;

    @Column(name = "security_updates", nullable = false)
    private boolean securityUpdates = true;

    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications = true;

    public UserNotificationSettings(UserAccount user) {
        this.user = user;
    }

    public void update(Boolean orderUpdates, Boolean depositUpdates, Boolean ticketUpdates, Boolean walletUpdates,
            Boolean securityUpdates, Boolean emailNotifications) {
        if (orderUpdates != null) {
            this.orderUpdates = orderUpdates;
        }
        if (depositUpdates != null) {
            this.depositUpdates = depositUpdates;
        }
        if (ticketUpdates != null) {
            this.ticketUpdates = ticketUpdates;
        }
        if (walletUpdates != null) {
            this.walletUpdates = walletUpdates;
        }
        if (securityUpdates != null) {
            this.securityUpdates = securityUpdates;
        }
        if (emailNotifications != null) {
            this.emailNotifications = emailNotifications;
        }
    }
}
