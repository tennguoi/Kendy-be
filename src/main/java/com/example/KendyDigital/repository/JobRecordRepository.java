package com.example.KendyDigital.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.JobRecord;

public interface JobRecordRepository extends JpaRepository<JobRecord, Long> {
    List<JobRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
