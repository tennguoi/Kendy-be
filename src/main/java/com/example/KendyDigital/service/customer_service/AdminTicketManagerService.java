package com.example.KendyDigital.service.customer_service;

import com.example.KendyDigital.dto.ticket.request.AdminTicketUpdateRequest;
import com.example.KendyDigital.dto.ticket.request.CreateTicketMessageRequest;
import com.example.KendyDigital.dto.ticket.response.TicketResponse;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketStatus;
import java.util.List;
import java.util.Map;

public interface AdminTicketManagerService {
    List<TicketResponse> listForAdmin(TicketStatus status, Long userId, Integer limit);
    List<TicketResponse> searchForAdmin(String query, TicketStatus status, TicketCategory category,
            TicketPriority priority, Long userId, Integer limit);
    TicketResponse getForAdmin(String ticketCode);
    TicketResponse addAdminMessage(Long adminUserId, String ticketCode, CreateTicketMessageRequest request);
    TicketResponse updateForAdmin(Long adminUserId, String ticketCode, AdminTicketUpdateRequest request);
    TicketResponse updatePriority(Long adminUserId, String ticketCode, TicketPriority priority);
    TicketResponse updateCategory(Long adminUserId, String ticketCode, TicketCategory category);
    List<TicketResponse> listUnassigned(Integer limit);
    List<TicketResponse> listAssignedToMe(Long adminUserId, Integer limit);
    Map<String, Object> resolutionTime();
}
