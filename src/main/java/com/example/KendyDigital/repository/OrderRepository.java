package com.example.KendyDigital.repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.example.KendyDigital.model.OrderRecord;
import com.example.KendyDigital.model.OrderStatus;

public interface OrderRepository extends JpaRepository<OrderRecord, Long> {
    boolean existsByOrderCode(String orderCode);

    Optional<OrderRecord> findByOrderCode(String orderCode);

    Optional<OrderRecord> findByUser_IdAndIdempotencyKey(Long userId, String idempotencyKey);

    List<OrderRecord> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<OrderRecord> findAllByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status, Pageable pageable);

    List<OrderRecord> findAllByService_IdOrderByCreatedAtDesc(Long serviceId, Pageable pageable);

    List<OrderRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<OrderRecord> findAllByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @Query("""
            select o from OrderRecord o
            join o.service s
            where o.user.id = :userId
              and (:status is null or o.status = :status)
              and (
                :queryPattern is null
                or lower(o.orderCode) like :queryPattern
                or lower(s.name) like :queryPattern
                or lower(s.slug) like :queryPattern
                or lower(coalesce(o.inputData, '')) like :queryPattern
                or (:exactId is not null and o.id = :exactId)
              )
            order by o.createdAt desc
            """)
    List<OrderRecord> searchUser(@Param("userId") Long userId, @Param("queryPattern") String queryPattern,
            @Param("exactId") Long exactId, @Param("status") OrderStatus status, Pageable pageable);

    @Query("""
            select o from OrderRecord o
            join o.user u
            join o.service s
            where (:status is null or o.status = :status)
              and (:userId is null or u.id = :userId)
              and (
                :queryPattern is null
                or lower(o.orderCode) like :queryPattern
                or lower(u.email) like :queryPattern
                or lower(u.name) like :queryPattern
                or lower(s.name) like :queryPattern
                or lower(s.slug) like :queryPattern
                or (:exactId is not null and o.id = :exactId)
              )
            order by o.createdAt desc
            """)
    List<OrderRecord> searchAdmin(@Param("queryPattern") String queryPattern, @Param("exactId") Long exactId,
            @Param("status") OrderStatus status, @Param("userId") Long userId, Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByUser_Id(Long userId);

    long countByUser_IdAndStatus(Long userId, OrderStatus status);

    long countByService_Id(Long serviceId);

    long countByService_IdAndStatus(Long serviceId, OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderRecord o join fetch o.user where o.orderCode = :orderCode")
    Optional<OrderRecord> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    @Query("select coalesce(sum(o.amount), 0) from OrderRecord o where o.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") OrderStatus status);

    @Query("select coalesce(sum(o.amount), 0) from OrderRecord o where o.status = :status and o.createdAt >= :from and o.createdAt < :to")
    BigDecimal sumAmountByStatusBetween(@Param("status") OrderStatus status, @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);

    @Query("""
            select o.service.id, o.service.name, count(o), coalesce(sum(o.amount), 0)
            from OrderRecord o
            group by o.service.id, o.service.name
            order by count(o) desc
            """)
    List<Object[]> servicePerformance(Pageable pageable);

    @Query("select coalesce(sum(o.service.costPrice), 0) from OrderRecord o where o.status = :status")
    BigDecimal sumCostPriceByStatus(@Param("status") OrderStatus status);

    @Query("select coalesce(sum(o.service.costPrice), 0) from OrderRecord o where o.status = :status and o.createdAt >= :from and o.createdAt < :to")
    BigDecimal sumCostPriceByStatusBetween(@Param("status") OrderStatus status, @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);
}
