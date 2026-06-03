package com.example.KendyDigital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.UserNotification;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<UserNotification> findByIdAndUser_Id(Long id, Long userId);

    long countByUser_IdAndReadAtIsNull(Long userId);
}
