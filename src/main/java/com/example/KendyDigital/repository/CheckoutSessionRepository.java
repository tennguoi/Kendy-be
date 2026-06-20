package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.checkout.CheckoutSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, Long> {
    boolean existsByCheckoutCode(String checkoutCode);

    Optional<CheckoutSession> findByCheckoutCode(String checkoutCode);

    Optional<CheckoutSession> findByUser_IdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from CheckoutSession c
            join fetch c.user
            join fetch c.service
            join fetch c.depositRequest
            where c.depositRequest.id = :depositId
            """)
    Optional<CheckoutSession> findByDepositRequestIdForUpdate(@Param("depositId") Long depositId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from CheckoutSession c
            join fetch c.user
            join fetch c.service
            join fetch c.depositRequest
            where c.checkoutCode = :checkoutCode
            """)
    Optional<CheckoutSession> findByCheckoutCodeForUpdate(@Param("checkoutCode") String checkoutCode);
}
