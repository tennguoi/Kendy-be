package com.example.KendyDigital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.KendyDigital.model.ServiceItem;
import com.example.KendyDigital.model.UserFavoriteService;

public interface UserFavoriteServiceRepository extends JpaRepository<UserFavoriteService, Long> {
    Optional<UserFavoriteService> findByUser_IdAndService_Id(Long userId, Long serviceId);

    boolean existsByUser_IdAndService_Id(Long userId, Long serviceId);

    List<UserFavoriteService> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
            select o.service from OrderRecord o
            where o.user.id = :userId
            group by o.service
            order by max(o.createdAt) desc
            """)
    List<ServiceItem> findRecentServices(@Param("userId") Long userId, Pageable pageable);
}
