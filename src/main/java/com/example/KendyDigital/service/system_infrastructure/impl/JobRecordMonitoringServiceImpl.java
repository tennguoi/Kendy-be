package com.example.KendyDigital.service.system_infrastructure.impl;

import com.example.KendyDigital.dto.monitoring.response.JobRecordResponse;
import com.example.KendyDigital.model.job.JobRecord;
import com.example.KendyDigital.repository.JobRecordRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.system_infrastructure.JobRecordMonitoringService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JobRecordMonitoringServiceImpl implements JobRecordMonitoringService {
    private final JobRecordRepository jobRecordRepository;
    private final AuditService auditService;

    public JobRecordMonitoringServiceImpl(JobRecordRepository jobRecordRepository, AuditService auditService) {
        this.jobRecordRepository = jobRecordRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobRecordResponse> jobs(Integer limit) {
        return jobRecordRepository.findAllByOrderByCreatedAtDesc(page(limit))
                .stream()
                .map(JobRecordResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobRecordResponse job(Long jobId) {
        return JobRecordResponse.from(requireJob(jobId));
    }

    @Override
    @Transactional
    public JobRecordResponse retryJob(Long adminUserId, Long jobId) {
        JobRecord job = requireJob(jobId);
        job.retry();
        auditService.recordAdmin(adminUserId, "JOB_RETRY_REQUESTED", "JOB", job.getId(), "name=" + job.getName());
        return JobRecordResponse.from(job);
    }

    @Override
    @Transactional
    public JobRecordResponse cancelJob(Long adminUserId, Long jobId) {
        JobRecord job = requireJob(jobId);
        job.cancel();
        auditService.recordAdmin(adminUserId, "JOB_CANCELLED", "JOB", job.getId(), "name=" + job.getName());
        return JobRecordResponse.from(job);
    }

    private JobRecord requireJob(Long jobId) {
        return jobRecordRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        return PageRequest.of(0, normalizedLimit);
    }
}
