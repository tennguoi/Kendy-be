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
            where :query is null or lower(s.key) like lower(concat('%', :query, '%'))
            order by s.key asc
            """)
    List<SystemSetting> search(@Param("query") String query, Pageable pageable);
}
