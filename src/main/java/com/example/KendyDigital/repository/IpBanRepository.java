package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.security.IpBan;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IpBanRepository extends JpaRepository<IpBan, Long> {

    @Query("""
            select b from IpBan b
            where b.ipOrCidr = :ipOrCidr and b.revokedAt is null
              and (b.expiresAt is null or b.expiresAt > :now)
            order by b.createdAt desc
            """)
    List<IpBan> findActiveByIpOrCidr(@Param("ipOrCidr") String ipOrCidr, @Param("now") Instant now);

    @Query("""
            select b from IpBan b
            where b.ipOrCidr = :ipOrCidr and b.revokedAt is null
            order by b.createdAt desc
            """)
    List<IpBan> findByIpOrCidrAndRevokedAtIsNull(@Param("ipOrCidr") String ipOrCidr);

    @Query("""
            select b from IpBan b
            where b.revokedAt is null and (b.expiresAt is null or b.expiresAt > :now)
            order by b.createdAt desc
            """)
    List<IpBan> findActive(@Param("now") Instant now, Pageable pageable);

    @Query("""
            select b from IpBan b
            where b.revokedAt is null and b.expiresAt is not null and b.expiresAt <= :now
            """)
    List<IpBan> findExpired(@Param("now") Instant now);

    long countByRevokedAtIsNullAndExpiresAtAfter(Instant now);
}
