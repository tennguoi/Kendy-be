package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.admin.AdminRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRoleRepository extends JpaRepository<AdminRole, Long> {
    Optional<AdminRole> findByName(String name);

    boolean existsByName(String name);
}
