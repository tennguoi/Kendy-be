package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.notification.EmailLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long>, JpaSpecificationExecutor<EmailLog> {
    List<EmailLog> findAllByToEmailIgnoreCaseOrderByCreatedAtDesc(String toEmail, Pageable pageable);

}

