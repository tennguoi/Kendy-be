package com.example.KendyDigital.service.deposit;

import com.example.KendyDigital.model.deposit.DepositRequest;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.service.audit.AuditService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositExpirationJob {
    private static final int BATCH_SIZE = 200;

    private final DepositRequestRepository depositRequestRepository;
    private final AuditService auditService;
    private final EntityManager entityManager;

    public DepositExpirationJob(DepositRequestRepository depositRequestRepository,
            AuditService auditService, EntityManager entityManager) {
        this.depositRequestRepository = depositRequestRepository;
        this.auditService = auditService;
        this.entityManager = entityManager;
    }

    @Scheduled(fixedDelayString = "${app.deposit.expiration-interval-ms:60000}")
    @Transactional
    public void expirePendingDeposits() {
        Instant now = Instant.now();
        while (true) {
            List<DepositRequest> expired =
                    depositRequestRepository.findPendingExpiredForUpdate(now, PageRequest.of(0, BATCH_SIZE));
            expired.forEach(deposit -> {
                deposit.markExpired();
                auditService.recordSystem("DEPOSIT_EXPIRED", "DEPOSIT_REQUEST", deposit.getId(), null);
            });
            if (expired.size() < BATCH_SIZE) {
                return;
            }
            entityManager.flush();
            entityManager.clear();
        }
    }
}
