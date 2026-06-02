package com.example.KendyDigital.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserStatus;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UserAccount> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<UserAccount> findAllByStatusOrderByCreatedAtDesc(UserStatus status, Pageable pageable);

    long countByStatus(UserStatus status);

    @Query("select coalesce(sum(u.balance), 0) from UserAccount u")
    BigDecimal sumAllBalances();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> findByIdForUpdate(@Param("id") Long id);
}
