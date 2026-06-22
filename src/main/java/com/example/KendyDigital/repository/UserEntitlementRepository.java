package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.entitlement.EntitlementStatus;
import com.example.KendyDigital.model.entitlement.UserEntitlement;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserEntitlementRepository extends JpaRepository<UserEntitlement, Long> {
    boolean existsBySourceOrder_Id(Long orderId);

    @Query("""
            select e from UserEntitlement e
            join fetch e.service
            join fetch e.sourceOrder o
            left join fetch o.deliveredCredential
            where e.user.id = :userId
            order by e.createdAt desc
            """)
    List<UserEntitlement> findAllForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select e from UserEntitlement e
            join fetch e.user
            join fetch e.service
            join fetch e.sourceOrder o
            left join fetch o.deliveredCredential
            order by e.createdAt desc
            """)
    List<UserEntitlement> findAllForAdmin(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from UserEntitlement e
            join fetch e.service
            join fetch e.sourceOrder o
            left join fetch o.deliveredCredential
            where e.id = :id and e.user.id = :userId
            """)
    Optional<UserEntitlement> findByIdForUserUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from UserEntitlement e
            join fetch e.service
            join fetch e.sourceOrder o
            left join fetch o.deliveredCredential
            where e.id = :id
            """)
    Optional<UserEntitlement> findByIdForUpdate(@Param("id") Long id);

    Optional<UserEntitlement> findBySourceOrder_Id(Long orderId);

    List<UserEntitlement> findAllByStatusInAndExpiresAtIsNotNullAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            List<EntitlementStatus> statuses, Instant expiresAt, Pageable pageable);

    List<UserEntitlement> findAllByStatusAndExpiresAtIsNotNullAndExpiresAtBetweenOrderByExpiresAtAsc(
            EntitlementStatus status, Instant from, Instant to, Pageable pageable);
}
