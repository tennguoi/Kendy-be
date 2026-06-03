package com.example.KendyDigital.repository;


import com.example.KendyDigital.model.*;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    Optional<BankTransaction> findByReferenceCode(String referenceCode);

    Optional<BankTransaction> findBySepayId(Long sepayId);

    List<BankTransaction> findAllByOrderByReceivedAtDesc(Pageable pageable);

    List<BankTransaction> findAllByStatusOrderByReceivedAtDesc(BankTransactionStatus status, Pageable pageable);

    @Query("""
            select b from BankTransaction b
            where (:status is null or b.status = :status)
              and (
                :query is null
                or lower(coalesce(b.referenceCode, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(b.code, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(b.content, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(b.accountNumber, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(b.gateway, '')) like lower(concat('%', :query, '%'))
                or (:exactId is not null and b.id = :exactId)
                or (:sepayId is not null and b.sepayId = :sepayId)
              )
            order by b.receivedAt desc
            """)
    List<BankTransaction> searchAdmin(@Param("query") String query, @Param("exactId") Long exactId,
            @Param("sepayId") Long sepayId, @Param("status") BankTransactionStatus status, Pageable pageable);

    long countByStatus(BankTransactionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BankTransaction b where b.id = :id")
    Optional<BankTransaction> findByIdForUpdate(@Param("id") Long id);
}
