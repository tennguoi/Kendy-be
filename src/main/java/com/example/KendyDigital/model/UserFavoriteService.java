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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_favorite_services",
        indexes = {
                @Index(name = "idx_user_favorite_services_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_favorite_service", columnNames = {"user_id", "service_id"})
        })
public class UserFavoriteService extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceItem service;

    public UserFavoriteService(UserAccount user, ServiceItem service) {
        this.user = user;
        this.service = service;
    }
}
