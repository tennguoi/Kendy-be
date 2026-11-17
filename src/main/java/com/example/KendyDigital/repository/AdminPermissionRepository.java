package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.admin.AdminPermission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminPermissionRepository extends JpaRepository<AdminPermission, Long> {
    Optional<AdminPermission> findByPublicId(UUID publicId);
    Optional<AdminPermission> findByCode(String code);

    boolean existsByCode(String code);

    List<AdminPermission> findAllByPublicIdIn(Collection<UUID> publicIds);
}
