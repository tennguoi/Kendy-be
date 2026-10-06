package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.security.AlertStatus;
import com.example.KendyDigital.model.security.AlertSubjectType;
import com.example.KendyDigital.model.security.SecurityAlert;
import com.example.KendyDigital.model.security.SecuritySeverity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {

    List<SecurityAlert> findAllByOrderByLastSeenDesc(Pageable pageable);

    List<SecurityAlert> findAllByStatusOrderByLastSeenDesc(AlertStatus status, Pageable pageable);

    List<SecurityAlert> findAllBySeverityOrderByLastSeenDesc(SecuritySeverity severity, Pageable pageable);

    List<SecurityAlert> findAllByStatusAndSeverityOrderByLastSeenDesc(AlertStatus status, SecuritySeverity severity,
            Pageable pageable);

    Optional<SecurityAlert> findFirstByRuleCodeAndSubjectTypeAndSubjectValueAndStatusOrderByLastSeenDesc(
            String ruleCode, AlertSubjectType subjectType, String subjectValue, AlertStatus status);

    long countByStatus(AlertStatus status);

    long countByStatusAndSeverity(AlertStatus status, SecuritySeverity severity);
}
