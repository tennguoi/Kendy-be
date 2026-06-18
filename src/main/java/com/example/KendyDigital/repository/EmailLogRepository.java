package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.notification.EmailLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findAllByToEmailIgnoreCaseOrderByCreatedAtDesc(String toEmail, Pageable pageable);

    @Query("""
            select e from EmailLog e
            where (cast(:queryPattern as string) is null
              or lower(e.toEmail) like :queryPattern
              or lower(e.subject) like :queryPattern)
              and (cast(:status as string) is null or e.status = :status)
            order by e.createdAt desc
            """)
    List<EmailLog> searchLogs(@Param("queryPattern") String queryPattern, @Param("status") String status, Pageable pageable);
}
