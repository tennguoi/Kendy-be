package com.example.KendyDigital.service.system_infrastructure.impl;

import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;
import com.example.KendyDigital.service.system_infrastructure.UserActivityMonitoringService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserActivityMonitoringServiceImpl implements UserActivityMonitoringService {
    private final OrderRepository orderRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TicketRepository ticketRepository;
    private final UserAccountRepository userAccountRepository;

    public UserActivityMonitoringServiceImpl(
            OrderRepository orderRepository,
            WalletTransactionRepository walletTransactionRepository,
            TicketRepository ticketRepository,
            UserAccountRepository userAccountRepository) {
        this.orderRepository = orderRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.ticketRepository = ticketRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listUserOrders(UUID userId, int page, int size) {
        Long internalUserId = requireUser(userId).getId();
        return orderRepository.findAllByUser_IdOrderByCreatedAtDesc(internalUserId, paged(page, size))
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listUserOrders(UUID userId, Integer limit) {
        return listUserOrders(userId, 0, limit == null ? 100 : limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listUserWalletTransactions(UUID userId, int page, int size) {
        Long internalUserId = requireUser(userId).getId();
        return walletTransactionRepository.findAllByUser_IdOrderByCreatedAtDesc(internalUserId, paged(page, size))
                .stream()
                .map(WalletTransactionResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listUserWalletTransactions(UUID userId, Integer limit) {
        return listUserWalletTransactions(userId, 0, limit == null ? 100 : limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listUserTickets(UUID userId, int page, int size) {
        Long internalUserId = requireUser(userId).getId();
        return ticketRepository.findAllByUser_IdOrderByCreatedAtDesc(internalUserId, paged(page, size))
                .stream()
                .map(ticket -> TicketResponse.from(ticket, List.of()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listUserTickets(UUID userId, Integer limit) {
        return listUserTickets(userId, 0, limit == null ? 100 : limit);
    }

    private UserAccount requireUser(UUID publicId) {
        return userAccountRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 500)));
    }
}
