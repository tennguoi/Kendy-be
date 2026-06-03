package com.example.KendyDigital.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.RolePermission;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findAllByRole_Id(Long roleId);

    void deleteByRole_Id(Long roleId);

    boolean existsByRole_IdAndPermission_Id(Long roleId, Long permissionId);
}
