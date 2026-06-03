package com.example.KendyDigital.model;

import com.example.KendyDigital.common.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_permissions")
public class AdminPermission extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 32)
    private String module;

    public AdminPermission(String code, String description, String module) {
        this.code = code;
        this.description = description;
        this.module = module;
    }
}
