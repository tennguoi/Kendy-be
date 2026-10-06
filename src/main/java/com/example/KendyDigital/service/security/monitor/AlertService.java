package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.model.security.AlertStatus;
import com.example.KendyDigital.model.security.AlertSubjectType;
import com.example.KendyDigital.model.security.SecurityAlert;
import com.example.KendyDigital.model.security.SecuritySeverity;
import java.util.List;

public interface AlertService {
    SecurityAlert raise(String ruleCode, SecuritySeverity severity, String title, AlertSubjectType subjectType,
            String subjectValue, String detail);

    List<SecurityAlert> list(AlertStatus status, SecuritySeverity severity, int limit);

    SecurityAlert updateStatus(Long id, AlertStatus status, String assignee, String note);

    long countOpen();

    List<SecurityAlert> recent(int limit);
}
