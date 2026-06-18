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

    @Query("""
            select t from Ticket t
            join fetch t.user u
            left join fetch t.order o
            left join fetch t.depositRequest d
            where t.user.id = :userId
              and (cast(:status as string) is null or t.status = :status)
              and (cast(:category as string) is null or t.category = :category)
              and (cast(:priority as string) is null or t.priority = :priority)
              and (
                cast(:queryPattern as string) is null
                or lower(t.ticketCode) like :queryPattern
                or lower(t.subject) like :queryPattern
                or lower(coalesce(o.orderCode, '')) like :queryPattern
                or lower(coalesce(d.depositCode, '')) like :queryPattern
                or (cast(:exactId as long) is not null and t.id = :exactId)
              )
            order by t.createdAt desc
            """)
    List<Ticket> searchUser(@Param("userId") Long userId, @Param("queryPattern") String queryPattern,
            @Param("exactId") Long exactId, @Param("status") TicketStatus status,
            @Param("category") TicketCategory category, @Param("priority") TicketPriority priority,
            Pageable pageable);

    @Query("""
            select t from Ticket t
            join fetch t.user u
            left join fetch t.order o
            left join fetch t.depositRequest d
            where (cast(:status as string) is null or t.status = :status)
              and (cast(:category as string) is null or t.category = :category)
              and (cast(:priority as string) is null or t.priority = :priority)
              and (cast(:userId as long) is null or u.id = :userId)
              and (
                cast(:queryPattern as string) is null
                or lower(t.ticketCode) like :queryPattern
                or lower(t.subject) like :queryPattern
                or lower(u.email) like :queryPattern
                or lower(u.name) like :queryPattern
                or lower(coalesce(o.orderCode, '')) like :queryPattern
                or lower(coalesce(d.depositCode, '')) like :queryPattern
                or (cast(:exactId as long) is not null and t.id = :exactId)
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
