package com.example.KendyDigital.model.admin;

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
        name = "role_permissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_role_permission", columnNames = { "role_id", "permission_id" })
        },
        indexes = {
                @Index(name = "idx_role_permissions_role_id", columnList = "role_id")
        })
public class RolePermission extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private AdminRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private AdminPermission permission;

    public RolePermission(AdminRole role, AdminPermission permission) {
        this.role = role;
        this.permission = permission;
    }
}
