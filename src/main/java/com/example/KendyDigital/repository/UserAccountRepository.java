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
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByOauthProviderAndOauthProviderId(String oauthProvider, String oauthProviderId);

    boolean existsByEmailIgnoreCase(String email);

    List<UserAccount> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<UserAccount> findAllByStatusOrderByCreatedAtDesc(UserStatus status, Pageable pageable);

    List<UserAccount> findAllByRoleInOrderByCreatedAtDesc(List<UserRole> roles, Pageable pageable);

    @Query("""
            select u from UserAccount u
            where (:status is null or u.status = :status)
              and (
                :queryPattern is null
                or lower(u.name) like :queryPattern
                or lower(u.email) like :queryPattern
                or lower(coalesce(u.phone, '')) like :queryPattern
                or (:exactId is not null and u.id = :exactId)
              )
            order by u.createdAt desc
            """)
    List<UserAccount> searchAdmin(@Param("queryPattern") String queryPattern, @Param("exactId") Long exactId,
            @Param("status") UserStatus status, Pageable pageable);

    long countByStatus(UserStatus status);

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
