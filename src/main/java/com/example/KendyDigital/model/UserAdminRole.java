package com.example.KendyDigital.model;

import com.example.KendyDigital.common.TimestampedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_admin_roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_admin_role", columnNames = { "user_id", "role_id" })
        },
        indexes = {
                @Index(name = "idx_user_admin_roles_user_id", columnList = "user_id")
        })
public class UserAdminRole extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private AdminRole role;

    public UserAdminRole(UserAccount user, AdminRole role) {
        this.user = user;
        this.role = role;
    }
}
