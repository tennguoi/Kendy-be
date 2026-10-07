package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.wallet.WalletTransaction;
import com.example.KendyDigital.model.wallet.WalletTransactionDirection;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long>, JpaSpecificationExecutor<WalletTransaction> {
    @Override
    @EntityGraph(attributePaths = "user")
    Page<WalletTransaction> findAll(Specification<WalletTransaction> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "user")
    List<WalletTransaction> findAll(Specification<WalletTransaction> spec);

    boolean existsByTransactionCode(String transactionCode);

    @Query("select w from WalletTransaction w join fetch w.user u where u.id = :userId order by w.createdAt desc")
    List<WalletTransaction> findAllByUser_IdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("select w from WalletTransaction w join fetch w.user u where u.id = :userId and w.type = :type order by w.createdAt desc")
    List<WalletTransaction> findAllByUser_IdAndTypeOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("type") WalletTransactionType type, Pageable pageable);

    @Query("select w from WalletTransaction w join fetch w.user u where u.id = :userId and w.direction = :direction order by w.createdAt desc")
    List<WalletTransaction> findAllByUser_IdAndDirectionOrderByCreatedAtDesc(@Param("userId") Long userId,
            @Param("direction") WalletTransactionDirection direction, Pageable pageable);

    @Query("select w from WalletTransaction w join fetch w.user u where u.id = :userId and w.type = :type and w.direction = :direction order by w.createdAt desc")
    List<WalletTransaction> findAllByUser_IdAndTypeAndDirectionOrderByCreatedAtDesc(@Param("userId") Long userId,
            @Param("type") WalletTransactionType type, @Param("direction") WalletTransactionDirection direction, Pageable pageable);

    @Query("select w from WalletTransaction w join fetch w.user u order by w.createdAt desc")
    List<WalletTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select w from WalletTransaction w join fetch w.user u where w.id = :id and u.id = :userId")
    Optional<WalletTransaction> findByIdAndUser_Id(@Param("id") Long id, @Param("userId") Long userId);

    long countByUser_Id(Long userId);

    @Query("select coalesce(sum(w.amount), 0) from WalletTransaction w where w.user.id = :userId and w.type = :type")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") Long userId, @Param("type") WalletTransactionType type);

    @Query("select coalesce(sum(w.amount), 0) from WalletTransaction w where w.type = :type and w.direction = :direction")
    BigDecimal sumAmountByTypeAndDirection(@Param("type") WalletTransactionType type, @Param("direction") WalletTransactionDirection direction);

    @Query("""
            select coalesce(sum(w.amount), 0) from WalletTransaction w
            where w.type = :type and w.direction = :direction and w.createdAt >= :from and w.createdAt < :to
            """)
    BigDecimal sumAmountByTypeAndDirectionBetween(@Param("type") WalletTransactionType type,
            @Param("direction") WalletTransactionDirection direction, @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);

    @Query(value = """
            select cast(created_at as date) as day, coalesce(sum(amount), 0)
            from wallet_transactions
            where created_at >= :from and created_at < :to
              and type = 'DEPOSIT' and direction = 'CREDIT'
            group by cast(created_at as date)
            order by day
            """, nativeQuery = true)
    List<Object[]> sumDepositsByDay(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);
}
