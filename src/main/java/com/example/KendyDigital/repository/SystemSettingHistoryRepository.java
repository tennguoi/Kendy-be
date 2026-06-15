package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.setting.SystemSettingHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingHistoryRepository extends JpaRepository<SystemSettingHistory, Long> {
    List<SystemSettingHistory> findAllByKeyOrderByCreatedAtDesc(String key, Pageable pageable);
}
