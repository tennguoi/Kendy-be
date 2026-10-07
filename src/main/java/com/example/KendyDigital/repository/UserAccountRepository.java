package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.model.user.UserStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByOauthProviderAndOauthProviderId(String oauthProvider, String oauthProviderId);

    boolean existsByEmailIgnoreCase(String email);

    List<UserAccount> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<UserAccount> findAllByStatusOrderByCreatedAtDesc(UserStatus status, Pageable pageable);

    List<UserAccount> findAllByRoleInOrderByCreatedAtDesc(List<UserRole> roles, Pageable pageable);

    List<UserAccount> findByRoleIn(List<UserRole> roles);

    long countByStatus(UserStatus status);

    long countByLockedUntilGreaterThan(Instant now);

    long countByFailedLoginAttemptsGreaterThan(int attempts);

    long countByRoleIn(List<UserRole> roles);

    long countByRoleInAndTwoFactorEnabledFalse(List<UserRole> roles);

    @Query("""
            select u from UserAccount u
            where u.failedLoginAttempts > 0
               or (u.lockedUntil is not null and u.lockedUntil > :now)
            order by u.failedLoginAttempts desc, u.id desc
            """)
    List<UserAccount> findRiskyAccounts(@Param("now") Instant now, Pageable pageable);

    long countByCreatedAtGreaterThanEqual(Instant from);

    @Query("select coalesce(sum(u.balance), 0) from UserAccount u")
    BigDecimal sumAllBalances();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id in :ids")
    List<UserAccount> findAllByIdForUpdate(@Param("ids") List<Long> ids);
}
