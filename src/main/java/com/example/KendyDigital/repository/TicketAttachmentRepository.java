package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.ticket.TicketAttachment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    List<TicketAttachment> findAllByTicket_TicketCodeOrderByCreatedAtDesc(String ticketCode);

    Optional<TicketAttachment> findByTicket_TicketCodeAndPublicId(String ticketCode, UUID publicId);
}
