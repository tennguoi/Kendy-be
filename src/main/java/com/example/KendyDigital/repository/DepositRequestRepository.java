package com.example.KendyDigital.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.example.KendyDigital.model.DepositRequest;
import com.example.KendyDigital.model.DepositStatus;

public interface DepositRequestRepository extends JpaRepository<DepositRequest, Long> {
    boolean existsByDepositCode(String depositCode);

    Optional<DepositRequest> findByDepositCode(String depositCode);

    List<DepositRequest> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<DepositRequest> findAllByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, DepositStatus status, Pageable pageable);

    List<DepositRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<DepositRequest> findAllByStatusOrderByCreatedAtDesc(DepositStatus status, Pageable pageable);

    @Query("""
            select d from DepositRequest d
            join d.user u
            where (:status is null or d.status = :status)
              and (:userId is null or u.id = :userId)
              and (
                :queryPattern is null
                or lower(d.depositCode) like :queryPattern
                or lower(d.transferContent) like :queryPattern
                or lower(d.bankAccount) like :queryPattern
                or lower(u.email) like :queryPattern
                or lower(u.name) like :queryPattern
                or (:exactId is not null and d.id = :exactId)
              )
            order by d.createdAt desc
            """)
    List<DepositRequest> searchAdmin(@Param("queryPattern") String queryPattern, @Param("exactId") Long exactId,
            @Param("status") DepositStatus status, @Param("userId") Long userId, Pageable pageable);

    long countByStatus(DepositStatus status);

    long countByUser_Id(Long userId);

    long countByUser_IdAndStatus(Long userId, DepositStatus status);

    long countByCreatedAtGreaterThanEqual(Instant from);

    @Query("select coalesce(sum(d.amount), 0) from DepositRequest d where d.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") DepositStatus status);

    @Query("select coalesce(sum(d.amount), 0) from DepositRequest d where d.user.id = :userId and d.status = :status")
    BigDecimal sumAmountByUserIdAndStatus(@Param("userId") Long userId, @Param("status") DepositStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DepositRequest d join fetch d.user where d.depositCode = :depositCode")
    Optional<DepositRequest> findByDepositCodeForUpdate(@Param("depositCode") String depositCode);
}
