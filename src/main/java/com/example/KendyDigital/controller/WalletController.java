package com.example.KendyDigital.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.wallet.response.WalletSummaryResponse;
import com.example.KendyDigital.dto.wallet.response.WalletTransactionResponse;
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
            @RequestParam(required = false) WalletTransactionDirection direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return walletService.listTransactions(CurrentUser.require(authentication).userId(), type, direction, page, size);
    }

    @GetMapping("/transactions/search")
    public List<WalletTransactionResponse> searchTransactions(Authentication authentication,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) WalletTransactionType type,
            @RequestParam(required = false) WalletTransactionDirection direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return walletService.searchTransactions(CurrentUser.require(authentication).userId(), query, type, direction,
                page, size);
    }

    @GetMapping("/transactions/{id}")
    public WalletTransactionResponse getTransaction(Authentication authentication, @PathVariable Long id) {
        return walletService.getTransaction(CurrentUser.require(authentication).userId(), id);
    }
}
