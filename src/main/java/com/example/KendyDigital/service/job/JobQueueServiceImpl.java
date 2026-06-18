package com.example.KendyDigital.service.job;

import com.example.KendyDigital.model.job.JobRecord;
import com.example.KendyDigital.model.job.JobStatus;
import com.example.KendyDigital.repository.JobRecordRepository;
import java.util.concurrent.CompletableFuture;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class JobQueueServiceImpl  implements JobQueueService{
    private final JobRecordRepository jobRecordRepository;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate requiresNewTransaction;

    public JobQueueServiceImpl(JobRecordRepository jobRecordRepository,
            PlatformTransactionManager transactionManager) {
        this.jobRecordRepository = jobRecordRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Async("jobExecutor")
    public CompletableFuture<JobRecord> execute(String name, JobTask task) {
        return runJob(name, task);
    }

    @Async("jobExecutor")
    public void executeAndForget(String name, JobTask task) {
        runJob(name, task);
    }

    private CompletableFuture<JobRecord> runJob(String name, JobTask task) {
        JobRecord created = requiresNewTransaction.execute(status ->
                jobRecordRepository.save(new JobRecord(name, JobStatus.RUNNING, instant() + " Started")));
        if (created == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Cannot create job record"));
        }
        try {
            JobRecord completed = transactionTemplate.execute(status -> {
                JobRecord job = requireJob(created.getId());
                try {
                    task.run(job);
                } catch (Exception exception) {
                    throw new JobExecutionException(exception);
                }
                job.complete("Completed successfully");
                return jobRecordRepository.save(job);
            });
            return CompletableFuture.completedFuture(completed);
        } catch (Exception e) {
            Throwable cause = e instanceof JobExecutionException && e.getCause() != null ? e.getCause() : e;
            requiresNewTransaction.executeWithoutResult(status -> {
                JobRecord failed = requireJob(created.getId());
                failed.fail("Error: " + safeMessage(cause));
                jobRecordRepository.save(failed);
            });
            return CompletableFuture.failedFuture(cause);
        }
    }

    private JobRecord requireJob(Long id) {
        return jobRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Job record not found: " + id));
    }

    private String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null || throwable.getMessage().isBlank()
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
    }

    private String instant() {
        return java.time.Instant.now().toString();
    }

    private static final class JobExecutionException extends RuntimeException {
        private JobExecutionException(Throwable cause) {
            super(cause);
        }
    }
}
