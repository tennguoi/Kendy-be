package com.example.KendyDigital.service.system_infrastructure;

import com.example.KendyDigital.dto.monitoring.response.JobRecordResponse;
import java.util.List;
import java.util.UUID;

public interface JobRecordMonitoringService {
    List<JobRecordResponse> jobs(Integer limit);
    JobRecordResponse job(UUID jobId);
    JobRecordResponse retryJob(Long adminUserId, UUID jobId);
    JobRecordResponse cancelJob(Long adminUserId, UUID jobId);
}
