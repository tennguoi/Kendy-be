package com.example.KendyDigital.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.KendyDigital.model.SystemSetting;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
    @Query("""
            select s from SystemSetting s
            where :queryPattern is null or lower(s.key) like :queryPattern
            order by s.key asc
            """)
    List<SystemSetting> search(@Param("queryPattern") String queryPattern, Pageable pageable);
}
