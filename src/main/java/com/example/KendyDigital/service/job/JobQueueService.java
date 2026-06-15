package com.example.KendyDigital.service.job;

import com.example.KendyDigital.model.job.JobRecord;
import java.util.concurrent.CompletableFuture;

public interface JobQueueService {
    CompletableFuture<JobRecord> execute(String name, JobTask task);
    void executeAndForget(String name, JobTask task);

    @FunctionalInterface
    interface JobTask {
        void run(JobRecord job) throws Exception;
    }
}
