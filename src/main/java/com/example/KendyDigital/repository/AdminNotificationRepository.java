package com.example.KendyDigital.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.AdminNotification;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    List<AdminNotification> findAllByAdminUserIdIsNullOrAdminUserIdOrderByCreatedAtDesc(Long adminUserId,
            Pageable pageable);
}
