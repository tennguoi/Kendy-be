package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.job.JobRecord;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRecordRepository extends JpaRepository<JobRecord, Long> {
    List<JobRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
