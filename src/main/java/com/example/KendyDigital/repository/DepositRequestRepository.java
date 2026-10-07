package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.deposit.DepositRequest;
import com.example.KendyDigital.model.deposit.DepositStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepositRequestRepository extends JpaRepository<DepositRequest, Long>, JpaSpecificationExecutor<DepositRequest> {
    @Override
    @EntityGraph(attributePaths = "user")
    Page<DepositRequest> findAll(Specification<DepositRequest> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "user")
    List<DepositRequest> findAll(Specification<DepositRequest> spec);

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
