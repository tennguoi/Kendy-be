package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.bank.BankTransaction;
import com.example.KendyDigital.model.bank.BankTransactionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long>, JpaSpecificationExecutor<BankTransaction> {
    Optional<BankTransaction> findByReferenceCode(String referenceCode);

    Optional<BankTransaction> findBySepayId(Long sepayId);

    List<BankTransaction> findAllByOrderByReceivedAtDesc(Pageable pageable);

    List<BankTransaction> findAllByStatusOrderByReceivedAtDesc(BankTransactionStatus status, Pageable pageable);

    long countByStatus(BankTransactionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BankTransaction b where b.id = :id")
    Optional<BankTransaction> findByIdForUpdate(@Param("id") Long id);
}
