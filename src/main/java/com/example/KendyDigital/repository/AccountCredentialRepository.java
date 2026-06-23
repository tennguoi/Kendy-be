package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountCredentialRepository extends JpaRepository<AccountCredential, Long> {
    boolean existsByService_IdAndLoginIdentifierIgnoreCase(Long serviceId, String loginIdentifier);

    boolean existsByService_IdAndPayloadHash(Long serviceId, String payloadHash);

    long countByService_IdAndStatus(Long serviceId, AccountCredentialStatus status);

    @EntityGraph(attributePaths = {"service", "assignedOrder", "reservedCheckout", "reservedByUser", "deliveredToUser"})
    List<AccountCredential> findAllByService_IdOrderByCreatedAtDesc(Long serviceId, Pageable pageable);

    @EntityGraph(attributePaths = {"service", "assignedOrder", "reservedCheckout", "reservedByUser", "deliveredToUser"})
    List<AccountCredential> findAllByService_IdAndStatusOrderByCreatedAtDesc(Long serviceId,
            AccountCredentialStatus status, Pageable pageable);

    @Query("""
            select c from AccountCredential c
            join fetch c.service
            left join fetch c.assignedOrder
            left join fetch c.reservedCheckout
            left join fetch c.reservedByUser
            left join fetch c.deliveredToUser
            where c.service.id = :serviceId
              and (cast(:status as string) is null or c.status = :status)
              and (cast(:queryPattern as string) is null
                or lower(c.loginIdentifier) like :queryPattern
                or lower(coalesce(c.internalNote, '')) like :queryPattern
              )
              and (cast(:createdFrom as timestamp) is null or c.createdAt >= :createdFrom)
              and (cast(:createdTo as timestamp) is null or c.createdAt < :createdTo)
              and (cast(:deliveredFrom as timestamp) is null or c.deliveredAt >= :deliveredFrom)
              and (cast(:deliveredTo as timestamp) is null or c.deliveredAt < :deliveredTo)
              and (cast(:expiresBefore as timestamp) is null or c.expiresAt < :expiresBefore)
            order by c.createdAt desc
            """)
    List<AccountCredential> searchForAdmin(@Param("serviceId") Long serviceId,
            @Param("status") AccountCredentialStatus status,
            @Param("queryPattern") String queryPattern,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            @Param("deliveredFrom") Instant deliveredFrom,
            @Param("deliveredTo") Instant deliveredTo,
            @Param("expiresBefore") Instant expiresBefore,
            Pageable pageable);

    @Query("""
            select c from AccountCredential c
            join fetch c.service
            join fetch c.deliveredToUser
            left join fetch c.assignedOrder
            where c.deliveredToUser is not null
              and (cast(:status as string) is null or c.status = :status)
              and (cast(:queryPattern as string) is null
                or lower(c.loginIdentifier) like :queryPattern
                or lower(c.service.name) like :queryPattern
                or lower(c.deliveredToUser.name) like :queryPattern
                or lower(c.deliveredToUser.email) like :queryPattern
                or lower(coalesce(c.deliveredToUser.phone, '')) like :queryPattern
                or lower(coalesce(c.assignedOrder.orderCode, '')) like :queryPattern
              )
              and (cast(:deliveredFrom as timestamp) is null or c.deliveredAt >= :deliveredFrom)
              and (cast(:deliveredTo as timestamp) is null or c.deliveredAt < :deliveredTo)
              and (cast(:expiresBefore as timestamp) is null or c.expiresAt < :expiresBefore)
            order by c.deliveredAt desc, c.createdAt desc
            """)
    List<AccountCredential> searchAssignedForAdmin(
            @Param("status") AccountCredentialStatus status,
            @Param("queryPattern") String queryPattern,
            @Param("deliveredFrom") Instant deliveredFrom,
            @Param("deliveredTo") Instant deliveredTo,
            @Param("expiresBefore") Instant expiresBefore,
            Pageable pageable);

    @Query("""
            select c from AccountCredential c
            join fetch c.service
            left join fetch c.assignedOrder
            where c.deliveredToUser.id = :userId
            order by c.deliveredAt desc, c.createdAt desc
            """)
    List<AccountCredential> findAllDeliveredForUser(@Param("userId") Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from AccountCredential c
            where c.service.id = :serviceId
              and c.status = com.example.KendyDigital.model.inventory.AccountCredentialStatus.AVAILABLE
            order by c.createdAt asc
            """)
    List<AccountCredential> findAvailableForDelivery(@Param("serviceId") Long serviceId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from AccountCredential c
            where c.service.id = :serviceId
              and c.reservedCheckout.id = :checkoutId
              and c.status = com.example.KendyDigital.model.inventory.AccountCredentialStatus.RESERVED
            """)
    Optional<AccountCredential> findReservedForCheckout(@Param("serviceId") Long serviceId,
            @Param("checkoutId") Long checkoutId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from AccountCredential c
            where c.reservedCheckout.id = :checkoutId
              and c.status = com.example.KendyDigital.model.inventory.AccountCredentialStatus.RESERVED
            """)
    Optional<AccountCredential> findReservedByCheckoutId(@Param("checkoutId") Long checkoutId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from AccountCredential c
            join fetch c.service
            where c.status = com.example.KendyDigital.model.inventory.AccountCredentialStatus.RESERVED
              and c.reservedUntil is not null
              and c.reservedUntil < :now
            order by c.reservedUntil asc
            """)
    List<AccountCredential> findExpiredReservations(@Param("now") Instant now, Pageable pageable);

    @Query("""
            select count(c) from AccountCredential c
            where c.expiresAt is not null
              and c.expiresAt >= :now
              and c.expiresAt < :until
              and c.status in (
                com.example.KendyDigital.model.inventory.AccountCredentialStatus.AVAILABLE,
                com.example.KendyDigital.model.inventory.AccountCredentialStatus.RESERVED,
                com.example.KendyDigital.model.inventory.AccountCredentialStatus.DELIVERED
              )
            """)
    long countExpiringCredentials(@Param("now") Instant now, @Param("until") Instant until);

    @Query("""
            select count(s) from ServiceItem s
            where s.type = com.example.KendyDigital.model.catalog.ServiceType.ACCOUNT_STOCK
              and s.status = com.example.KendyDigital.model.catalog.ServiceStatus.ACTIVE
              and (
                select count(c) from AccountCredential c
                where c.service = s
                  and c.status = com.example.KendyDigital.model.inventory.AccountCredentialStatus.AVAILABLE
              ) <= :threshold
            """)
    long countLowStockServices(@Param("threshold") long threshold);
}
