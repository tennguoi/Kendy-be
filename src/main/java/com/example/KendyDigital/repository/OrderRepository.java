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

    List<OrderRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<OrderRecord> findAllByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);


    long countByStatus(OrderStatus status);

    long countByUser_Id(Long userId);

    long countByUser_IdAndStatus(Long userId, OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderRecord o join fetch o.user where o.orderCode = :orderCode")
    Optional<OrderRecord> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    @Query("select coalesce(sum(o.amount), 0) from OrderRecord o where o.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") OrderStatus status);
}
