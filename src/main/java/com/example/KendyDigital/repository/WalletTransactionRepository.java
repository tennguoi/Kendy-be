package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.wallet.WalletTransaction;
import com.example.KendyDigital.model.wallet.WalletTransactionDirection;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
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

    @Query("""
            select w from WalletTransaction w
            join fetch w.user u
            where u.id = :userId
              and (cast(:type as string) is null or w.type = :type)
              and (cast(:direction as string) is null or w.direction = :direction)
              and (
                cast(:queryPattern as string) is null
                or lower(w.transactionCode) like :queryPattern
                or lower(coalesce(w.referenceType, '')) like :queryPattern
                or lower(coalesce(w.description, '')) like :queryPattern
                or (cast(:exactId as long) is not null and w.id = :exactId)
                or (cast(:exactId as long) is not null and w.referenceId = :exactId)
              )
            order by w.createdAt desc
            """)
    List<WalletTransaction> searchUser(@Param("userId") Long userId, @Param("queryPattern") String queryPattern,
            @Param("exactId") Long exactId, @Param("type") WalletTransactionType type,
            @Param("direction") WalletTransactionDirection direction, Pageable pageable);

    @Query("""
            select w from WalletTransaction w
            join fetch w.user u
            where (cast(:userId as long) is null or u.id = :userId)
              and (cast(:type as string) is null or w.type = :type)
              and (cast(:direction as string) is null or w.direction = :direction)
              and (
                cast(:queryPattern as string) is null
                or lower(w.transactionCode) like :queryPattern
                or lower(coalesce(w.referenceType, '')) like :queryPattern
                or lower(coalesce(w.description, '')) like :queryPattern
                or lower(u.email) like :queryPattern
                or lower(u.name) like :queryPattern
                or (cast(:exactId as long) is not null and w.id = :exactId)
                or (cast(:exactId as long) is not null and w.referenceId = :exactId)
              )
            order by w.createdAt desc
            """)
    List<WalletTransaction> searchAdmin(@Param("queryPattern") String queryPattern, @Param("exactId") Long exactId,
            @Param("userId") Long userId, @Param("type") WalletTransactionType type,
            @Param("direction") WalletTransactionDirection direction, Pageable pageable);

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
}
