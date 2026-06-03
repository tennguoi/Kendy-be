package com.example.KendyDigital.repository;


import com.example.KendyDigital.model.*;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    List<AuthSession> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<AuthSession> findByIdAndUser_Id(Long id, Long userId);
}
