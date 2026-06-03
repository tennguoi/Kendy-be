package com.example.KendyDigital.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.TicketAttachment;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    List<TicketAttachment> findAllByTicket_TicketCodeOrderByCreatedAtDesc(String ticketCode);
}
