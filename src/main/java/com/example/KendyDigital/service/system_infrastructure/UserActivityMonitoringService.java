package com.example.KendyDigital.service.system_infrastructure;

import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
import java.util.List;

public interface UserActivityMonitoringService {
    List<OrderResponse> listUserOrders(Long userId, int page, int size);
    List<OrderResponse> listUserOrders(Long userId, Integer limit);
    List<WalletTransactionResponse> listUserWalletTransactions(Long userId, int page, int size);
    List<WalletTransactionResponse> listUserWalletTransactions(Long userId, Integer limit);
    List<TicketResponse> listUserTickets(Long userId, int page, int size);
    List<TicketResponse> listUserTickets(Long userId, Integer limit);
}
