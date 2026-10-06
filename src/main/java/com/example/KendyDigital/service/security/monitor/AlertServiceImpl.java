package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.model.security.AlertStatus;
import com.example.KendyDigital.model.security.AlertSubjectType;
import com.example.KendyDigital.model.security.SecurityAlert;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.repository.SecurityAlertRepository;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlertServiceImpl implements AlertService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertServiceImpl.class);
    private static final Duration NOTIFY_DEDUP_WINDOW = Duration.ofMinutes(15);

    private final SecurityAlertRepository alertRepository;
    private final SecurityCounters counters;
    private final AlertNotifier alertNotifier;

    public AlertServiceImpl(SecurityAlertRepository alertRepository, SecurityCounters counters,
            AlertNotifier alertNotifier) {
        this.alertRepository = alertRepository;
        this.counters = counters;
        this.alertNotifier = alertNotifier;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAlert raise(String ruleCode, SecuritySeverity severity, String title,
            AlertSubjectType subjectType, String subjectValue, String detail) {
        String subject = subjectValue == null ? "unknown" : subjectValue;
        SecurityAlert alert = alertRepository
                .findFirstByRuleCodeAndSubjectTypeAndSubjectValueAndStatusOrderByLastSeenDesc(
                        ruleCode, subjectType, subject, AlertStatus.OPEN)
                .orElse(null);

        boolean created = false;
        if (alert == null) {
            alert = new SecurityAlert(ruleCode, severity, title, subjectType, subject, detail);
            created = true;
        } else {
            alert.registerOccurrence();
            alert.setSeverity(severity);
            if (detail != null) {
                alert.setMetadata(detail);
            }
        }
        SecurityAlert saved = alertRepository.save(alert);

        boolean shouldNotify = created
                || counters.setIfAbsent("sec:alertnotify:" + ruleCode + ":" + subject, NOTIFY_DEDUP_WINDOW);
        if (shouldNotify) {
            try {
                alertNotifier.notify(severity, ruleCode, title, detail);
            } catch (RuntimeException exception) {
                LOGGER.debug("Alert notification failed for {}", ruleCode, exception);
            }
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityAlert> list(AlertStatus status, SecuritySeverity severity, int limit) {
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, 200)));
        if (status != null && severity != null) {
            return alertRepository.findAllByStatusAndSeverityOrderByLastSeenDesc(status, severity, page);
        }
        if (status != null) {
            return alertRepository.findAllByStatusOrderByLastSeenDesc(status, page);
        }
        if (severity != null) {
            return alertRepository.findAllBySeverityOrderByLastSeenDesc(severity, page);
        }
        return alertRepository.findAllByOrderByLastSeenDesc(page);
    }

    @Override
    @Transactional
    public SecurityAlert updateStatus(Long id, AlertStatus status, String assignee, String note) {
        SecurityAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        alert.resolve(status, assignee, note);
        return alert;
    }

    @Override
    @Transactional(readOnly = true)
    public long countOpen() {
        return alertRepository.countByStatus(AlertStatus.OPEN);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityAlert> recent(int limit) {
        return alertRepository.findAllByOrderByLastSeenDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 50))));
    }
}
