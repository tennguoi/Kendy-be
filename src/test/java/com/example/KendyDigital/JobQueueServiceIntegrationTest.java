package com.example.KendyDigital;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.KendyDigital.model.job.JobStatus;
import com.example.KendyDigital.repository.JobRecordRepository;
import com.example.KendyDigital.service.job.JobQueueService;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JobQueueServiceIntegrationTest {
    @Autowired private JobQueueService jobQueueService;
    @Autowired private JobRecordRepository jobRecordRepository;

    @Test
    void persistsCompletedStatus() {
        var job = jobQueueService.execute("successful-test-job", ignored -> {
        }).join();

        assertEquals(JobStatus.COMPLETED, jobRecordRepository.findById(job.getId()).orElseThrow().getStatus());
    }

    @Test
    void persistsFailedStatusOutsideRolledBackTaskTransaction() {
        assertThrows(CompletionException.class,
                () -> jobQueueService.execute("failed-test-job", ignored -> {
                    throw new IllegalStateException("expected failure");
                }).join());

        var job = jobRecordRepository.findAllByOrderByCreatedAtDesc(
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .stream()
                .filter(record -> record.getName().equals("failed-test-job"))
                .findFirst()
                .orElseThrow();
        assertEquals(JobStatus.FAILED, job.getStatus());
    }
}
