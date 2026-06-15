package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.ticket.TicketMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
    List<TicketMessage> findByTicket_TicketCodeOrderByCreatedAtAsc(String ticketCode);
}
