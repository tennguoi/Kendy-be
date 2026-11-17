package com.example.KendyDigital.service.system_infrastructure;

import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import java.util.List;
import java.util.UUID;

public interface UserActivityMonitoringService {
    List<OrderResponse> listUserOrders(UUID userId, int page, int size);
    List<OrderResponse> listUserOrders(UUID userId, Integer limit);
    List<WalletTransactionResponse> listUserWalletTransactions(UUID userId, int page, int size);
    List<WalletTransactionResponse> listUserWalletTransactions(UUID userId, Integer limit);
    List<TicketResponse> listUserTickets(UUID userId, int page, int size);
    List<TicketResponse> listUserTickets(UUID userId, Integer limit);
}
