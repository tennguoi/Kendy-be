package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.setting.SystemSettingHistory;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingHistoryRepository extends JpaRepository<SystemSettingHistory, Long> {
    Optional<SystemSettingHistory> findByPublicId(UUID publicId);
    List<SystemSettingHistory> findAllByKeyOrderByCreatedAtDesc(String key, Pageable pageable);
}
