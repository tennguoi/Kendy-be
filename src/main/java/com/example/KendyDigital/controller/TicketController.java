package com.example.KendyDigital.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.*;
import com.example.KendyDigital.model.TicketStatus;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.TicketService;

import jakarta.validation.Valid;

@RestController
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/api/tickets")
    public TicketResponse create(Authentication authentication, @Valid @RequestBody CreateTicketRequest request) {
        return ticketService.create(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/tickets")
    public List<TicketResponse> list(Authentication authentication,
            @RequestParam(required = false) TicketStatus status) {
        return ticketService.listForUser(CurrentUser.require(authentication).userId(), status);
    }

    @GetMapping("/api/tickets/{ticketCode}")
    public TicketResponse get(Authentication authentication, @PathVariable String ticketCode) {
        return ticketService.getForUser(CurrentUser.require(authentication).userId(), ticketCode);
    }

    @PostMapping("/api/tickets/{ticketCode}/messages")
    public TicketResponse addMessage(Authentication authentication, @PathVariable String ticketCode,
            @Valid @RequestBody CreateTicketMessageRequest request) {
        return ticketService.addUserMessage(CurrentUser.require(authentication).userId(), ticketCode, request);
    }

    @PostMapping("/api/tickets/{ticketCode}/close")
    public TicketResponse close(Authentication authentication, @PathVariable String ticketCode) {
        return ticketService.closeForUser(CurrentUser.require(authentication).userId(), ticketCode);
    }

    @GetMapping("/api/admin/tickets")
    public List<TicketResponse> listAdmin(@RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Long userId) {
        return ticketService.listForAdmin(status, userId);
    }

    @GetMapping("/api/admin/tickets/{ticketCode}")
    public TicketResponse getAdmin(@PathVariable String ticketCode) {
        return ticketService.getForAdmin(ticketCode);
    }

    @PostMapping("/api/admin/tickets/{ticketCode}/messages")
    public TicketResponse addAdminMessage(Authentication authentication, @PathVariable String ticketCode,
            @Valid @RequestBody CreateTicketMessageRequest request) {
        return ticketService.addAdminMessage(CurrentUser.require(authentication).userId(), ticketCode, request);
    }

    @PostMapping("/api/admin/tickets/{ticketCode}/update")
    public TicketResponse updateAdmin(Authentication authentication, @PathVariable String ticketCode,
            @Valid @RequestBody AdminTicketUpdateRequest request) {
        return ticketService.updateForAdmin(CurrentUser.require(authentication).userId(), ticketCode, request);
    }
}
