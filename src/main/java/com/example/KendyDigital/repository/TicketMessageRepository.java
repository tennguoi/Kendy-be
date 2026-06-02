package com.example.KendyDigital.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import com.example.KendyDigital.model.TicketMessage;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
    List<TicketMessage> findByTicket_TicketCodeOrderByCreatedAtAsc(String ticketCode);
}
