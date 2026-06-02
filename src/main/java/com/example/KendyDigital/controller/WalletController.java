package com.example.KendyDigital.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.WalletSummaryResponse;
import com.example.KendyDigital.dto.WalletTransactionResponse;
import com.example.KendyDigital.model.WalletTransactionDirection;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.WalletService;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletSummaryResponse getWallet(Authentication authentication) {
        return walletService.getWallet(CurrentUser.require(authentication).userId());
    }

    @GetMapping("/transactions")
    public List<WalletTransactionResponse> listTransactions(Authentication authentication,
            @RequestParam(required = false) WalletTransactionType type,
            @RequestParam(required = false) WalletTransactionDirection direction) {
        return walletService.listTransactions(CurrentUser.require(authentication).userId(), type, direction);
    }
}
