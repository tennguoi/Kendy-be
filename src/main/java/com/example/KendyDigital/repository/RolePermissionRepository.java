package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.admin.RolePermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findAllByRole_Id(Long roleId);

    void deleteByRole_Id(Long roleId);

    boolean existsByRole_IdAndPermission_Id(Long roleId, Long permissionId);

    @Query("select rp from RolePermission rp join fetch rp.permission where rp.role.id in :roleIds")
    List<RolePermission> findAllByRole_IdInWithPermission(@Param("roleIds") List<Long> roleIds);

    @Query("""
            select distinct rp.permission.code from RolePermission rp
            where rp.role.id in (
                select ur.role.id from UserAdminRole ur where ur.user.id = :userId
            )
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);
}
