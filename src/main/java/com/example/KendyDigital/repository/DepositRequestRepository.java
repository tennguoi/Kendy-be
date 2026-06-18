package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.deposit.DepositRequest;
import com.example.KendyDigital.model.deposit.DepositStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepositRequestRepository extends JpaRepository<DepositRequest, Long> {
    boolean existsByDepositCode(String depositCode);

    Optional<DepositRequest> findByDepositCode(String depositCode);

    @Query("select d from DepositRequest d join fetch d.user where d.user.id = :userId order by d.createdAt desc")
    List<DepositRequest> findAllByUser_IdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("select d from DepositRequest d join fetch d.user where d.user.id = :userId and d.status = :status order by d.createdAt desc")
    List<DepositRequest> findAllByUser_IdAndStatusOrderByCreatedAtDesc(@Param("userId") Long userId,
            @Param("status") DepositStatus status, Pageable pageable);

    @Query("select d from DepositRequest d left join fetch d.user order by d.createdAt desc")
    List<DepositRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select d from DepositRequest d left join fetch d.user where d.status = :status order by d.createdAt desc")
    List<DepositRequest> findAllByStatusOrderByCreatedAtDesc(@Param("status") DepositStatus status, Pageable pageable);

    @Query("""
            select d from DepositRequest d
            join fetch d.user u
            where (cast(:status as string) is null or d.status = :status)
              and (cast(:userId as long) is null or u.id = :userId)
              and (cast(:fromDate as timestamp) is null or d.createdAt >= :fromDate)
              and (cast(:toDate as timestamp) is null or d.createdAt < :toDate)
              and (
                cast(:queryPattern as string) is null
                or lower(d.depositCode) like :queryPattern
                or lower(d.transferContent) like :queryPattern
                or lower(d.bankAccount) like :queryPattern
                or lower(u.email) like :queryPattern
                or lower(u.name) like :queryPattern
                or (cast(:exactId as long) is not null and d.id = :exactId)
              )
            order by d.createdAt desc
            """)
    List<DepositRequest> searchAdmin(@Param("queryPattern") String queryPattern, @Param("exactId") Long exactId,
            @Param("status") DepositStatus status, @Param("userId") Long userId,
            @Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d from DepositRequest d
            where d.status = com.example.KendyDigital.model.deposit.DepositStatus.PENDING
              and d.expiredAt < :now
            order by d.expiredAt asc
            """)
    List<DepositRequest> findPendingExpiredForUpdate(@Param("now") Instant now, Pageable pageable);

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
