package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.user.UserAdminRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAdminRoleRepository extends JpaRepository<UserAdminRole, Long> {
    List<UserAdminRole> findAllByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);
}
