package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.bank.BankTransaction;
import com.example.KendyDigital.model.bank.BankTransactionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    Optional<BankTransaction> findByReferenceCode(String referenceCode);

    Optional<BankTransaction> findBySepayId(Long sepayId);

    List<BankTransaction> findAllByOrderByReceivedAtDesc(Pageable pageable);

    List<BankTransaction> findAllByStatusOrderByReceivedAtDesc(BankTransactionStatus status, Pageable pageable);

    @Query("""
            select b from BankTransaction b
            where (cast(:status as string) is null or b.status = :status)
              and (
                cast(:queryPattern as string) is null
                or lower(coalesce(b.referenceCode, '')) like :queryPattern
                or lower(coalesce(b.code, '')) like :queryPattern
                or lower(coalesce(b.content, '')) like :queryPattern
                or lower(coalesce(b.accountNumber, '')) like :queryPattern
                or lower(coalesce(b.gateway, '')) like :queryPattern
                or (cast(:exactId as long) is not null and b.id = :exactId)
                or (cast(:sepayId as long) is not null and b.sepayId = :sepayId)
              )
            order by b.receivedAt desc
            """)
    List<BankTransaction> searchAdmin(@Param("queryPattern") String queryPattern, @Param("exactId") Long exactId,
            @Param("sepayId") Long sepayId, @Param("status") BankTransactionStatus status, Pageable pageable);

    long countByStatus(BankTransactionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BankTransaction b where b.id = :id")
    Optional<BankTransaction> findByIdForUpdate(@Param("id") Long id);
}
