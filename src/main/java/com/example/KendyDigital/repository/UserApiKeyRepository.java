package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.user.UserApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserApiKeyRepository extends JpaRepository<UserApiKey, Long> {
    Optional<UserApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);

    Optional<UserApiKey> findByIdAndUser_Id(Long id, Long userId);

    List<UserApiKey> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUser_IdAndRevokedAtIsNull(Long userId);

    long countByRevokedAtIsNull();

    @Query("""
            select k from UserApiKey k join fetch k.user
            where k.revokedAt is null
            order by k.createdAt desc
            """)
    List<UserApiKey> findActiveKeys(Pageable pageable);
}
