package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    @Override
    @EntityGraph(attributePaths = {"user", "order", "depositRequest"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "order", "depositRequest"})
    List<Ticket> findAll(Specification<Ticket> spec);

    boolean existsByTicketCode(String ticketCode);

    @Query("select t from Ticket t join fetch t.user u left join fetch t.order o left join fetch t.depositRequest d where t.ticketCode = :ticketCode")
    Optional<Ticket> findByTicketCode(@Param("ticketCode") String ticketCode);

    @Query("select t from Ticket t join fetch t.user u left join fetch t.order o left join fetch t.depositRequest d where u.id = :userId order by t.createdAt desc")
    List<Ticket> findAllByUser_IdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("select t from Ticket t join fetch t.user u left join fetch t.order o left join fetch t.depositRequest d where u.id = :userId and t.status = :status order by t.createdAt desc")
    List<Ticket> findAllByUser_IdAndStatusOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("status") TicketStatus status, Pageable pageable);

    @Query("select t from Ticket t join fetch t.user u left join fetch t.order o left join fetch t.depositRequest d where t.assignedAdmin is null order by t.createdAt desc")
    List<Ticket> findAllByAssignedAdminIsNullOrderByCreatedAtDesc(Pageable pageable);

    @Query("select t from Ticket t join fetch t.user u left join fetch t.order o left join fetch t.depositRequest d where t.assignedAdmin.id = :adminId order by t.createdAt desc")
    List<Ticket> findAllByAssignedAdmin_IdOrderByCreatedAtDesc(@Param("adminId") Long adminId, Pageable pageable);

    @Query("select t from Ticket t join fetch t.user u left join fetch t.order o left join fetch t.depositRequest d order by t.createdAt desc")
    List<Ticket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select t from Ticket t join fetch t.user u left join fetch t.order o left join fetch t.depositRequest d where t.status = :status order by t.createdAt desc")
    List<Ticket> findAllByStatusOrderByCreatedAtDesc(@Param("status") TicketStatus status, Pageable pageable);
    long countByStatus(TicketStatus status);

    long countByUser_Id(Long userId);

    long countByUser_IdAndStatus(Long userId, TicketStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t join fetch t.user where t.ticketCode = :ticketCode")
    Optional<Ticket> findByTicketCodeForUpdate(@Param("ticketCode") String ticketCode);
}
