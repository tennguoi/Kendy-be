package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderRecord, Long> {
    boolean existsByOrderCode(String orderCode);

    @Query("""
            select o from OrderRecord o
            join fetch o.service
            left join fetch o.deliveredCredential
            left join fetch o.assignedAdmin
            left join fetch o.supportTicket
            join fetch o.user
            where o.orderCode = :orderCode
            """)
    Optional<OrderRecord> findByOrderCode(@Param("orderCode") String orderCode);

    @Query("select o from OrderRecord o join fetch o.service join fetch o.user where o.user.id = :userId and o.idempotencyKey = :idempotencyKey")
    Optional<OrderRecord> findByUser_IdAndIdempotencyKey(@Param("userId") Long userId, @Param("idempotencyKey") String idempotencyKey);

    @Query("""
            select o from OrderRecord o
            join fetch o.service
            left join fetch o.deliveredCredential
            left join fetch o.assignedAdmin
            left join fetch o.supportTicket
            join fetch o.user
            where o.user.id = :userId
            order by o.createdAt desc
            """)
    List<OrderRecord> findAllByUser_IdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select o from OrderRecord o
            join fetch o.service
            left join fetch o.deliveredCredential
            left join fetch o.assignedAdmin
            left join fetch o.supportTicket
            join fetch o.user
            where o.user.id = :userId and o.status = :status
            order by o.createdAt desc
            """)
    List<OrderRecord> findAllByUser_IdAndStatusOrderByCreatedAtDesc(@Param("userId") Long userId,
            @Param("status") OrderStatus status, Pageable pageable);

    @Query("""
            select o from OrderRecord o
            join fetch o.service
            left join fetch o.deliveredCredential
            left join fetch o.assignedAdmin
            left join fetch o.supportTicket
            join fetch o.user
            where o.service.id = :serviceId
            order by o.createdAt desc
            """)
    List<OrderRecord> findAllByService_IdOrderByCreatedAtDesc(@Param("serviceId") Long serviceId, Pageable pageable);

    @Query("""
            select o from OrderRecord o
            join fetch o.service
            left join fetch o.deliveredCredential
            left join fetch o.assignedAdmin
            left join fetch o.supportTicket
            join fetch o.user
            order by o.createdAt desc
            """)
    List<OrderRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select o from OrderRecord o
            join fetch o.service
            left join fetch o.deliveredCredential
            left join fetch o.assignedAdmin
            left join fetch o.supportTicket
            join fetch o.user
            where o.status = :status
            order by o.createdAt desc
            """)
    List<OrderRecord> findAllByStatusOrderByCreatedAtDesc(@Param("status") OrderStatus status, Pageable pageable);

    @Query("""
            select o from OrderRecord o
            join fetch o.service s
            left join fetch o.deliveredCredential
            left join fetch o.assignedAdmin
            left join fetch o.supportTicket
            join fetch o.user u
            where u.id = :userId
              and (cast(:status as string) is null or o.status = :status)
              and (
                cast(:queryPattern as string) is null
                or lower(o.orderCode) like :queryPattern
                or lower(s.name) like :queryPattern
                or lower(s.slug) like :queryPattern
                or lower(coalesce(o.inputData, '')) like :queryPattern
                or (cast(:exactId as long) is not null and o.id = :exactId)
              )
            order by o.createdAt desc
            """)
    List<OrderRecord> searchUser(@Param("userId") Long userId, @Param("queryPattern") String queryPattern,
            @Param("exactId") Long exactId, @Param("status") OrderStatus status, Pageable pageable);

    @Query("""
            select o from OrderRecord o
            join fetch o.user u
            join fetch o.service s
            left join fetch o.deliveredCredential
            left join fetch o.assignedAdmin
            left join fetch o.supportTicket
            where (cast(:status as string) is null or o.status = :status)
              and (cast(:userId as long) is null or u.id = :userId)
              and (cast(:fromDate as timestamp) is null or o.createdAt >= :fromDate)
              and (cast(:toDate as timestamp) is null or o.createdAt < :toDate)
              and (
                cast(:queryPattern as string) is null
                or lower(o.orderCode) like :queryPattern
                or lower(u.email) like :queryPattern
                or lower(u.name) like :queryPattern
                or lower(s.name) like :queryPattern
                or lower(s.slug) like :queryPattern
                or (cast(:exactId as long) is not null and o.id = :exactId)
              )
            order by o.createdAt desc
            """)
    List<OrderRecord> searchAdmin(@Param("queryPattern") String queryPattern, @Param("exactId") Long exactId,
            @Param("status") OrderStatus status, @Param("userId") Long userId,
            @Param("fromDate") java.time.Instant fromDate, @Param("toDate") java.time.Instant toDate,
            Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByUser_Id(Long userId);

    long countByUser_IdAndStatus(Long userId, OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderRecord o join fetch o.user where o.orderCode = :orderCode")
    Optional<OrderRecord> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    long countByService_Id(Long serviceId);

    long countByService_IdAndStatus(Long serviceId, OrderStatus status);

    long countByService_TypeAndStatus(ServiceType serviceType, OrderStatus status);

    @Query("select coalesce(sum(o.amount), 0) from OrderRecord o where o.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") OrderStatus status);

    @Query("select coalesce(sum(o.amount), 0) from OrderRecord o where o.status = :status and o.createdAt >= :from and o.createdAt < :to")
    BigDecimal sumAmountByStatusBetween(@Param("status") OrderStatus status, @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);

    @Query(value = """
            select cast(created_at as date) as day, status, coalesce(sum(amount), 0)
            from orders
            where created_at >= :from and created_at < :to
              and status in ('COMPLETED', 'REFUNDED')
            group by cast(created_at as date), status
            order by day
            """, nativeQuery = true)
    List<Object[]> sumRevenueByDay(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);

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
