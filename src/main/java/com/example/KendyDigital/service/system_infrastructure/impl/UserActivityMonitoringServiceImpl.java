package com.example.KendyDigital.service.system_infrastructure.impl;

import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;
import com.example.KendyDigital.service.system_infrastructure.UserActivityMonitoringService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserActivityMonitoringServiceImpl implements UserActivityMonitoringService {
    private final OrderRepository orderRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TicketRepository ticketRepository;

    public UserActivityMonitoringServiceImpl(
            OrderRepository orderRepository,
            WalletTransactionRepository walletTransactionRepository,
            TicketRepository ticketRepository) {
        this.orderRepository = orderRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listUserOrders(Long userId, int page, int size) {
        return orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listUserOrders(Long userId, Integer limit) {
        return listUserOrders(userId, 0, limit == null ? 100 : limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listUserWalletTransactions(Long userId, int page, int size) {
        return walletTransactionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(WalletTransactionResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listUserWalletTransactions(Long userId, Integer limit) {
        return listUserWalletTransactions(userId, 0, limit == null ? 100 : limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listUserTickets(Long userId, int page, int size) {
        return ticketRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(ticket -> TicketResponse.from(ticket, List.of()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listUserTickets(Long userId, Integer limit) {
        return listUserTickets(userId, 0, limit == null ? 100 : limit);
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 500)));
    }
}
