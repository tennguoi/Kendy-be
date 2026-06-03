package com.example.KendyDigital.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.CancelDepositRequest;
import com.example.KendyDigital.dto.CreateDepositRequest;
import com.example.KendyDigital.dto.DepositResponse;
import com.example.KendyDigital.dto.DepositQrResponse;
import com.example.KendyDigital.dto.DepositStatusResponse;
import com.example.KendyDigital.model.DepositStatus;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.DepositService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/deposits")
public class DepositController {
    private final DepositService depositService;

    public DepositController(DepositService depositService) {
        this.depositService = depositService;
    }

    @PostMapping
    public DepositResponse createDeposit(Authentication authentication,
            @Valid @RequestBody CreateDepositRequest request) {
        return depositService.createDeposit(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping
    public List<DepositResponse> listDeposits(Authentication authentication,
            @RequestParam(required = false) DepositStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return depositService.listByUser(CurrentUser.require(authentication).userId(), status, page, size);
    }

    @GetMapping("/{depositCode}")
    public DepositResponse getDeposit(Authentication authentication, @PathVariable String depositCode) {
        return depositService.getDepositForUser(CurrentUser.require(authentication).userId(), depositCode);
    }

    @GetMapping("/{depositCode}/status")
    public DepositStatusResponse getDepositStatus(Authentication authentication, @PathVariable String depositCode) {
        return depositService.statusForUser(CurrentUser.require(authentication).userId(), depositCode);
    }

    @GetMapping("/{depositCode}/qr")
    public DepositQrResponse getDepositQr(Authentication authentication, @PathVariable String depositCode) {
        return depositService.qrForUser(CurrentUser.require(authentication).userId(), depositCode);
    }

    @PostMapping("/{depositCode}/cancel")
    public DepositResponse cancelDeposit(Authentication authentication, @PathVariable String depositCode,
            @RequestBody(required = false) CancelDepositRequest request) {
        return depositService.cancelForUser(CurrentUser.require(authentication).userId(), depositCode, request);
    }
}
