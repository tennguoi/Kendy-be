package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.security.SecurityEvent;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    long countByOccurredAtGreaterThanEqual(Instant from);

    long countBySeverityAndOccurredAtGreaterThanEqual(SecuritySeverity severity, Instant from);

    long countBySeverityInAndOccurredAtGreaterThanEqual(List<SecuritySeverity> severities, Instant from);

    long countByTypeAndOccurredAtGreaterThanEqual(SecurityEventType type, Instant from);

    @Query("""
            select e from SecurityEvent e
            where (:type is null or cast(e.type as string) = :type)
              and (:severity is null or cast(e.severity as string) = :severity)
              and (:ip is null or e.ip = :ip)
              and (cast(:userId as long) is null or e.userId = :userId)
              and (cast(:from as timestamp) is null or e.occurredAt >= :from)
              and (cast(:to as timestamp) is null or e.occurredAt <= :to)
            order by e.occurredAt desc
            """)
    List<SecurityEvent> searchSecurityEvents(@Param("type") String type,
            @Param("severity") String severity, @Param("ip") String ip, @Param("userId") Long userId,
            @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("""
            select e.ip as ip, count(e) as total,
                   max(e.severity) as maxSeverity
            from SecurityEvent e
            where e.occurredAt >= :from and e.ip is not null
            group by e.ip
            order by count(e) desc
            """)
    List<IpAggregate> findTopIps(@Param("from") Instant from, Pageable pageable);

    @Query("""
            select e.type as type, count(e) as total
            from SecurityEvent e
            where e.occurredAt >= :from
            group by e.type
            order by count(e) desc
            """)
    List<TypeAggregate> countByTypeSince(@Param("from") Instant from);

    @Query("""
            select e.occurredAt as occurredAt, e.type as type
            from SecurityEvent e
            where e.occurredAt >= :from
            order by e.occurredAt asc
            """)
    List<TimelineRow> findTimelineRowsSince(@Param("from") Instant from, Pageable pageable);

    @Modifying
    @Query("delete from SecurityEvent e where e.occurredAt < :cutoff and e.severity in :severities")
    int deleteByOccurredAtBeforeAndSeverityIn(@Param("cutoff") Instant cutoff,
            @Param("severities") List<SecuritySeverity> severities);

    interface IpAggregate {
        String getIp();

        long getTotal();

        SecuritySeverity getMaxSeverity();
    }

    interface TypeAggregate {
        SecurityEventType getType();

        long getTotal();
    }

    interface TimelineRow {
        Instant getOccurredAt();

        SecurityEventType getType();
    }
}
