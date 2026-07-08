package com.example.KendyDigital.model.order;

import com.example.KendyDigital.common.TimestampedEntity;
import com.example.KendyDigital.model.user.UserAccount;
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
        name = "manual_order_tasks",
        indexes = {
                @Index(name = "idx_manual_order_tasks_order_sort", columnList = "order_id,sort_order"),
                @Index(name = "idx_manual_order_tasks_completed", columnList = "completed")
        })
public class ManualOrderTask extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderRecord order;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_admin_id")
    private UserAccount completedByAdmin;

    public ManualOrderTask(OrderRecord order, String title, int sortOrder) {
        this.order = order;
        this.title = title;
        this.sortOrder = sortOrder;
    }

    public void update(String title, Integer sortOrder) {
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    public void markCompleted(UserAccount admin) {
        this.completed = true;
        this.completedAt = Instant.now();
        this.completedByAdmin = admin;
    }

    public void reopen() {
        this.completed = false;
        this.completedAt = null;
        this.completedByAdmin = null;
    }
}
