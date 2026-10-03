package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.auth.AuthSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    List<AuthSession> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<AuthSession> findByIdAndUser_Id(Long id, Long userId);

    List<AuthSession> findAllByUser_IdAndRevokedAtIsNull(Long userId);

    List<AuthSession> findAllByUser_IdAndRevokedAtIsNullOrderByCreatedAtAsc(Long userId);

    long countByUser_IdAndRevokedAtIsNull(Long userId);

    long countByRevokedAtIsNullAndExpiresAtAfter(Instant now);

    @Query("""
            select s from AuthSession s join fetch s.user
            where s.revokedAt is null and s.expiresAt > :now
            order by s.createdAt desc
            """)
    List<AuthSession> findActiveSessions(@Param("now") Instant now, Pageable pageable);
}
