package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.admin.AdminNotification;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    Optional<AdminNotification> findByPublicId(UUID publicId);
    List<AdminNotification> findAllByAdminUserIdIsNullOrAdminUserIdOrderByCreatedAtDesc(Long adminUserId,
            Pageable pageable);
}
