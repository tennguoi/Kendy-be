package com.example.KendyDigital.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.AuthUserResponse;
import com.example.KendyDigital.dto.ChangePasswordRequest;
import com.example.KendyDigital.dto.UpdateProfileRequest;
import com.example.KendyDigital.dto.UserDashboardResponse;
import com.example.KendyDigital.model.DepositStatus;
import com.example.KendyDigital.model.OrderStatus;
import com.example.KendyDigital.model.TicketStatus;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;

@Service
public class UserProfileService {
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final TicketRepository ticketRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserProfileService(UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            DepositRequestRepository depositRequestRepository,
            TicketRepository ticketRepository,
            WalletTransactionRepository walletTransactionRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.ticketRepository = ticketRepository;
        this.walletTransactionRepository = walletTransactionRepository;
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
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
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

    private UserAccount getUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
