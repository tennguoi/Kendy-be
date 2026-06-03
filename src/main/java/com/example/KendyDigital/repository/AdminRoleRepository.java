package com.example.KendyDigital.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.AdminRole;

public interface AdminRoleRepository extends JpaRepository<AdminRole, Long> {
    Optional<AdminRole> findByName(String name);

    boolean existsByName(String name);
}
