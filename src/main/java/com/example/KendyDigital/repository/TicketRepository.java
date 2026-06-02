package com.example.KendyDigital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.KendyDigital.model.*;

import jakarta.persistence.LockModeType;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByTicketCode(String ticketCode);

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Ticket> findAllByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, TicketStatus status, Pageable pageable);

    List<Ticket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Ticket> findAllByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);


    long countByStatus(TicketStatus status);

    long countByUser_Id(Long userId);

    long countByUser_IdAndStatus(Long userId, TicketStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t join fetch t.user where t.ticketCode = :ticketCode")
    Optional<Ticket> findByTicketCodeForUpdate(@Param("ticketCode") String ticketCode);
}
