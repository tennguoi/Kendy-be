package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.audit.AuditLog;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    java.util.Optional<AuditLog> findTopByOrderByIdDesc();

    List<AuditLog> findAllByOrderByIdAsc(Pageable pageable);

    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AuditLog> findAllByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    List<AuditLog> findAllByActorUserIdOrderByCreatedAtDesc(Long actorUserId, Pageable pageable);

    List<AuditLog> findAllByActionInOrderByCreatedAtDesc(Collection<String> actions, Pageable pageable);

    long countByActionInAndCreatedAtGreaterThanEqual(Collection<String> actions, Instant from);

}

