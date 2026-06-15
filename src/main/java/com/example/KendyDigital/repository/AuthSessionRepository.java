package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.auth.AuthSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    List<AuthSession> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<AuthSession> findByIdAndUser_Id(Long id, Long userId);

    List<AuthSession> findAllByUser_IdAndRevokedAtIsNull(Long userId);

    long countByUser_IdAndRevokedAtIsNull(Long userId);
}
