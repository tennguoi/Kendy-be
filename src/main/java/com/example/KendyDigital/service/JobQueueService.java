package com.example.KendyDigital.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.KendyDigital.model.JobRecord;
import com.example.KendyDigital.repository.JobRecordRepository;

@Service
public class JobQueueService {
    private final JobRecordRepository jobRecordRepository;

    public JobQueueService(JobRecordRepository jobRecordRepository) {
        this.jobRecordRepository = jobRecordRepository;
    }

    @Async("jobExecutor")
    @Transactional
    public CompletableFuture<JobRecord> execute(String name, JobTask task) {
        JobRecord job = jobRecordRepository.save(new JobRecord(name, "RUNNING", instant() + " Started"));
        try {
            task.run(job);
            job.setStatus("COMPLETED");
            appendLog(job, "Completed successfully");
            return CompletableFuture.completedFuture(jobRecordRepository.save(job));
        } catch (Exception e) {
            job.setStatus("FAILED");
            appendLog(job, "Error: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("jobExecutor")
    @Transactional
    public void executeAndForget(String name, JobTask task) {
        execute(name, task);
    }

    private void appendLog(JobRecord job, String line) {
        String prefix = job.getLogs() == null || job.getLogs().isBlank() ? "" : job.getLogs() + "\n";
        job.setLogs(prefix + instant() + " " + line);
    }

    private String instant() {
        return java.time.Instant.now().toString();
    }

    @FunctionalInterface
    public interface JobTask {
        void run(JobRecord job) throws Exception;
    }
}
