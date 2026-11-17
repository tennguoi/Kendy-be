package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.job.JobRecord;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRecordRepository extends JpaRepository<JobRecord, Long> {
    Optional<JobRecord> findByPublicId(UUID publicId);
    List<JobRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
