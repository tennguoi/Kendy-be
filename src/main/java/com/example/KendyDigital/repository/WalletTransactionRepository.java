package com.example.KendyDigital.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.KendyDigital.model.WalletTransaction;
import com.example.KendyDigital.model.WalletTransactionDirection;
import com.example.KendyDigital.model.WalletTransactionType;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    boolean existsByTransactionCode(String transactionCode);

    List<WalletTransaction> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<WalletTransaction> findAllByUser_IdAndTypeOrderByCreatedAtDesc(Long userId, WalletTransactionType type, Pageable pageable);

    List<WalletTransaction> findAllByUser_IdAndDirectionOrderByCreatedAtDesc(Long userId,
            WalletTransactionDirection direction, Pageable pageable);

    List<WalletTransaction> findAllByUser_IdAndTypeAndDirectionOrderByCreatedAtDesc(Long userId,
            WalletTransactionType type, WalletTransactionDirection direction, Pageable pageable);

    List<WalletTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);


    long countByUser_Id(Long userId);

    @Query("select coalesce(sum(w.amount), 0) from WalletTransaction w where w.user.id = :userId and w.type = :type")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") Long userId, @Param("type") WalletTransactionType type);

    @Query("select coalesce(sum(w.amount), 0) from WalletTransaction w where w.type = :type and w.direction = :direction")
    BigDecimal sumAmountByTypeAndDirection(@Param("type") WalletTransactionType type, @Param("direction") WalletTransactionDirection direction);
}
