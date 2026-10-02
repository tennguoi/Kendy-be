package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.user.UserAdminRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAdminRoleRepository extends JpaRepository<UserAdminRole, Long> {
    List<UserAdminRole> findAllByUser_Id(Long userId);

    @Modifying
    @Query("delete from UserAdminRole u where u.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);

    boolean existsByRole_Id(Long roleId);
}
