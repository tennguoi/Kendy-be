package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.audit.AuditLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AuditLog> findAllByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    List<AuditLog> findAllByActorUserIdOrderByCreatedAtDesc(Long actorUserId, Pageable pageable);

    @Query("""
            select a from AuditLog a
            where (cast(:action as string) is null or a.action = :action)
              and (cast(:actorUserId as long) is null or a.actorUserId = :actorUserId)
              and (cast(:targetType as string) is null or a.targetType = :targetType)
              and (cast(:targetId as long) is null or a.targetId = :targetId)
              and (
                cast(:queryPattern as string) is null
                or lower(a.action) like :queryPattern
                or lower(coalesce(a.targetType, '')) like :queryPattern
                or lower(coalesce(a.metadata, '')) like :queryPattern
              )
            order by a.createdAt desc
            """)
    List<AuditLog> searchAdmin(@Param("queryPattern") String queryPattern, @Param("action") String action,
            @Param("actorUserId") Long actorUserId, @Param("targetType") String targetType,
            @Param("targetId") Long targetId, Pageable pageable);
}
