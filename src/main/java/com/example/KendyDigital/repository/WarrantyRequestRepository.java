package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.warranty.WarrantyRequest;
import com.example.KendyDigital.model.warranty.WarrantyRequestStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarrantyRequestRepository extends JpaRepository<WarrantyRequest, Long> {
    List<WarrantyRequest> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<WarrantyRequest> findAllByStatusOrderByCreatedAtDesc(WarrantyRequestStatus status, Pageable pageable);

    List<WarrantyRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByOrder_IdAndStatusIn(Long orderId, Collection<WarrantyRequestStatus> statuses);

    long countByStatusIn(Collection<WarrantyRequestStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WarrantyRequest w join fetch w.order o join fetch w.user where w.id = :id")
    Optional<WarrantyRequest> findByIdForUpdate(@Param("id") Long id);
}
