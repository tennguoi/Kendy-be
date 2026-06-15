package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByTicketCode(String ticketCode);

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Ticket> findAllByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, TicketStatus status, Pageable pageable);

    List<Ticket> findAllByAssignedAdminIsNullOrderByCreatedAtDesc(Pageable pageable);

    List<Ticket> findAllByAssignedAdmin_IdOrderByCreatedAtDesc(Long adminId, Pageable pageable);

    List<Ticket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Ticket> findAllByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);

    @Query("""
            select t from Ticket t
            left join t.order o
            left join t.depositRequest d
            where t.user.id = :userId
              and (:status is null or t.status = :status)
              and (:category is null or t.category = :category)
              and (:priority is null or t.priority = :priority)
              and (
                :queryPattern is null
                or lower(t.ticketCode) like :queryPattern
                or lower(t.subject) like :queryPattern
                or lower(coalesce(o.orderCode, '')) like :queryPattern
                or lower(coalesce(d.depositCode, '')) like :queryPattern
                or (:exactId is not null and t.id = :exactId)
              )
            order by t.createdAt desc
            """)
    List<Ticket> searchUser(@Param("userId") Long userId, @Param("queryPattern") String queryPattern,
            @Param("exactId") Long exactId, @Param("status") TicketStatus status,
            @Param("category") TicketCategory category, @Param("priority") TicketPriority priority,
            Pageable pageable);

    @Query("""
            select t from Ticket t
            join t.user u
            left join t.order o
            left join t.depositRequest d
            where (:status is null or t.status = :status)
              and (:category is null or t.category = :category)
              and (:priority is null or t.priority = :priority)
              and (:userId is null or u.id = :userId)
              and (
                :queryPattern is null
                or lower(t.ticketCode) like :queryPattern
                or lower(t.subject) like :queryPattern
                or lower(u.email) like :queryPattern
                or lower(u.name) like :queryPattern
                or lower(coalesce(o.orderCode, '')) like :queryPattern
                or lower(coalesce(d.depositCode, '')) like :queryPattern
                or (:exactId is not null and t.id = :exactId)
              )
            order by t.createdAt desc
            """)
    List<Ticket> searchAdmin(@Param("queryPattern") String queryPattern, @Param("exactId") Long exactId,
            @Param("status") TicketStatus status, @Param("category") TicketCategory category,
            @Param("priority") TicketPriority priority, @Param("userId") Long userId, Pageable pageable);

    long countByStatus(TicketStatus status);

    long countByUser_Id(Long userId);

    long countByUser_IdAndStatus(Long userId, TicketStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t join fetch t.user where t.ticketCode = :ticketCode")
    Optional<Ticket> findByTicketCodeForUpdate(@Param("ticketCode") String ticketCode);
}
