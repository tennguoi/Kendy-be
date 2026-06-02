package com.example.KendyDigital.repository;

import java.math.BigDecimal;
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


    long countByStatus(DepositStatus status);

    long countByUser_Id(Long userId);

    long countByUser_IdAndStatus(Long userId, DepositStatus status);

    @Query("select coalesce(sum(d.amount), 0) from DepositRequest d where d.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") DepositStatus status);

    @Query("select coalesce(sum(d.amount), 0) from DepositRequest d where d.user.id = :userId and d.status = :status")
    BigDecimal sumAmountByUserIdAndStatus(@Param("userId") Long userId, @Param("status") DepositStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DepositRequest d join fetch d.user where d.depositCode = :depositCode")
    Optional<DepositRequest> findByDepositCodeForUpdate(@Param("depositCode") String depositCode);
}
