package com.example.KendyDigital.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.AdminPermission;

public interface AdminPermissionRepository extends JpaRepository<AdminPermission, Long> {
    Optional<AdminPermission> findByCode(String code);

    boolean existsByCode(String code);
}
