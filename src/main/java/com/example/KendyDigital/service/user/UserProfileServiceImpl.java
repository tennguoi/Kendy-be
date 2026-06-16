package com.example.KendyDigital.service.user;

import com.example.KendyDigital.dto.auth.response.AuthUserResponse;
import com.example.KendyDigital.dto.user.request.ChangePasswordRequest;
import com.example.KendyDigital.dto.user.request.UpdateProfileRequest;
import com.example.KendyDigital.dto.user.response.UserDashboardResponse;
import com.example.KendyDigital.model.deposit.DepositStatus;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.ticket.TicketStatus;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import com.example.KendyDigital.repository.AuthSessionRepository;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserApiKeyRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;
import com.example.KendyDigital.service.audit.AuditService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileServiceImpl  implements UserProfileService{
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final TicketRepository ticketRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AuthSessionRepository authSessionRepository;
    private final UserApiKeyRepository userApiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserProfileServiceImpl(UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            DepositRequestRepository depositRequestRepository,
            TicketRepository ticketRepository,
            WalletTransactionRepository walletTransactionRepository,
            AuthSessionRepository authSessionRepository,
            UserApiKeyRepository userApiKeyRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.ticketRepository = ticketRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.authSessionRepository = authSessionRepository;
        this.userApiKeyRepository = userApiKeyRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getProfile(Long userId) {
        return AuthUserResponse.from(getUser(userId));
    }

    @Transactional
    public AuthUserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserAccount user = getUser(userId);
        user.setName(request.name().trim());
        user.setPhone(blankToNull(request.phone()));
        auditService.recordSystem("USER_PROFILE_UPDATED", "USER", user.getId(), null);
        return AuthUserResponse.from(user);
    }

    @Transactional
    public AuthUserResponse changePassword(Long userId, ChangePasswordRequest request) {
        UserAccount user = getUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "New password must be different");
        }
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        auditService.recordSystem("USER_PASSWORD_CHANGED", "USER", user.getId(), null);
        return AuthUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserDashboardResponse dashboard(Long userId) {
        UserAccount user = getUser(userId);
        return new UserDashboardResponse(
                AuthUserResponse.from(user),
                user.getBalance(),
                orderRepository.countByUser_Id(userId),
                orderRepository.countByUser_IdAndStatus(userId, OrderStatus.PROCESSING),
                orderRepository.countByUser_IdAndStatus(userId, OrderStatus.COMPLETED),
                orderRepository.countByUser_IdAndStatus(userId, OrderStatus.CANCELLED),
                depositRequestRepository.countByUser_Id(userId),
                depositRequestRepository.countByUser_IdAndStatus(userId, DepositStatus.PENDING),
                depositRequestRepository.countByUser_IdAndStatus(userId, DepositStatus.COMPLETED),
                depositRequestRepository.sumAmountByUserIdAndStatus(userId, DepositStatus.COMPLETED),
                ticketRepository.countByUser_Id(userId),
                ticketRepository.countByUser_IdAndStatus(userId, TicketStatus.PENDING_ADMIN),
                ticketRepository.countByUser_IdAndStatus(userId, TicketStatus.PENDING_USER),
                walletTransactionRepository.countByUser_Id(userId),
                walletTransactionRepository.sumAmountByUserIdAndType(userId, WalletTransactionType.PURCHASE),
                walletTransactionRepository.sumAmountByUserIdAndType(userId, WalletTransactionType.REFUND));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportPersonalData(Long userId) {
        UserAccount user = getUser(userId);
        PageRequest page = PageRequest.of(0, 500);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportedAt", java.time.Instant.now().toString());
        data.put("profile", Map.of(
                "id", user.getId(),
                "publicId", String.valueOf(user.getPublicId()),
                "name", nullToBlank(user.getName()),
                "email", nullToBlank(user.getEmail()),
                "phone", nullToBlank(user.getPhone()),
                "role", String.valueOf(user.getRole()),
                "status", String.valueOf(user.getStatus()),
                "createdAt", user.getCreatedAt(),
                "updatedAt", user.getUpdatedAt()));
        data.put("orders", orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, page).stream()
                .map(order -> Map.of(
                        "orderCode", order.getOrderCode(),
                        "serviceName", order.getService().getName(),
                        "status", String.valueOf(order.getStatus()),
                        "amount", order.getAmount(),
                        "createdAt", order.getCreatedAt()))
                .toList());
        data.put("deposits", depositRequestRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, page).stream()
                .map(deposit -> Map.of(
                        "depositCode", deposit.getDepositCode(),
                        "status", String.valueOf(deposit.getStatus()),
                        "amount", deposit.getAmount(),
                        "createdAt", deposit.getCreatedAt()))
                .toList());
        data.put("walletTransactions", walletTransactionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, page)
                .stream()
                .map(transaction -> Map.of(
                        "transactionCode", transaction.getTransactionCode(),
                        "type", String.valueOf(transaction.getType()),
                        "direction", String.valueOf(transaction.getDirection()),
                        "amount", transaction.getAmount(),
                        "balanceAfter", transaction.getBalanceAfter(),
                        "createdAt", transaction.getCreatedAt()))
                .toList());
        data.put("tickets", ticketRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, page).stream()
                .map(ticket -> Map.of(
                        "ticketCode", ticket.getTicketCode(),
                        "subject", ticket.getSubject(),
                        "status", String.valueOf(ticket.getStatus()),
                        "createdAt", ticket.getCreatedAt()))
                .toList());
        data.put("loginSessions", authSessionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, page).stream()
                .map(session -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", session.getId());
                    item.put("createdAt", session.getCreatedAt());
                    item.put("lastUsedAt", session.getLastUsedAt());
                    item.put("expiresAt", session.getExpiresAt());
                    item.put("revokedAt", session.getRevokedAt());
                    return item;
                })
                .toList());
        return data;
    }

    @Transactional
    public Map<String, Object> deleteAccount(Long userId) {
        UserAccount user = getUser(userId);
        if (user.getStatus() == UserStatus.DELETED) {
            return Map.of("status", "DELETED");
        }
        authSessionRepository.findAllByUser_IdAndRevokedAtIsNull(userId).forEach(session -> session.revoke());
        userApiKeyRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500))
                .forEach(apiKey -> {
                    if (apiKey.getRevokedAt() == null) {
                        apiKey.revoke();
                    }
                });
        user.setName("Deleted user #" + user.getId());
        user.setEmail("deleted-user-" + user.getId() + "@example.invalid");
        user.setPhone(null);
        user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setEmailVerifiedAt(null);
        user.setOauthProvider(null);
        user.setOauthProviderId(null);
        user.setAvatarUrl(null);
        user.disableTwoFactor();
        user.setStatus(UserStatus.DELETED);
        auditService.recordSystem("USER_ACCOUNT_DELETED", "USER", user.getId(), "gdpr=anonymized");
        return Map.of("status", "DELETED");
    }

    private UserAccount getUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
