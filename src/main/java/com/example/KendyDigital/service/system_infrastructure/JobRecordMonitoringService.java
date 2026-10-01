package com.example.KendyDigital.service.system_infrastructure;

import com.example.KendyDigital.dto.monitoring.response.JobRecordResponse;
import java.util.List;

public interface JobRecordMonitoringService {
    List<JobRecordResponse> jobs(Integer limit);
    JobRecordResponse job(Long jobId);
    JobRecordResponse retryJob(Long adminUserId, Long jobId);
    JobRecordResponse cancelJob(Long adminUserId, Long jobId);
}
