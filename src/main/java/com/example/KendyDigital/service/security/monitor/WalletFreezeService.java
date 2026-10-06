package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controls the wallet freeze flag used during account-takeover response. A frozen wallet blocks
 * user-initiated debits (purchases) but still permits deposits, refunds and administrative
 * adjustments, so fraud recovery remains possible.
 */
@Service
public class WalletFreezeService {
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final UserNotificationService userNotificationService;

    public WalletFreezeService(UserAccountRepository userAccountRepository, AuditService auditService,
            UserNotificationService userNotificationService) {
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.userNotificationService = userNotificationService;
    }

    @Transactional(readOnly = true)
    public boolean isFrozen(Long userId) {
        return userAccountRepository.findById(userId).map(UserAccount::isWalletFrozen).orElse(false);
    }

    @Transactional
    public void freeze(Long userId, String reason, Long actorUserId) {
        UserAccount user = userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.isWalletFrozen()) {
            return;
        }
        user.freezeWallet(reason, actorUserId);
        userAccountRepository.save(user);
        if (actorUserId != null) {
            auditService.recordAdmin(actorUserId, "SECURITY_WALLET_FROZEN", "USER", userId, "reason=" + reason);
        } else {
            auditService.recordSystem("SECURITY_WALLET_FROZEN", "USER", userId, "reason=" + reason);
        }
        userNotificationService.create(userId, "Wallet temporarily frozen",
                "Your wallet was temporarily frozen for security reasons. Please contact support.",
                "SECURITY", "/account/security");
    }

    @Transactional
    public void unfreeze(Long userId, String reason, Long actorUserId) {
        UserAccount user = userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isWalletFrozen()) {
            return;
        }
        user.unfreezeWallet();
        userAccountRepository.save(user);
        if (actorUserId != null) {
            auditService.recordAdmin(actorUserId, "SECURITY_WALLET_UNFROZEN", "USER", userId, "reason=" + reason);
        } else {
            auditService.recordSystem("SECURITY_WALLET_UNFROZEN", "USER", userId, "reason=" + reason);
        }
        userNotificationService.create(userId, "Wallet unfrozen",
                "Your wallet has been unfrozen. You can use it normally again.", "SECURITY", "/account/security");
    }
}
