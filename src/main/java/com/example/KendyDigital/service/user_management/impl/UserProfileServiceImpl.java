package com.example.KendyDigital.service.user_management.impl;

import com.example.KendyDigital.dto.auth.response.AuthUserResponse;
import com.example.KendyDigital.dto.user.request.ChangePasswordRequest;
import com.example.KendyDigital.dto.user.request.SetPasswordRequest;
import com.example.KendyDigital.dto.user.request.UpdateProfileRequest;
import com.example.KendyDigital.dto.user.response.UserDashboardResponse;
import com.example.KendyDigital.model.deposit.DepositStatus;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.ticket.TicketStatus;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.user_management.UserAvatarService;
import com.example.KendyDigital.service.user_management.UserDataPortabilityService;
import com.example.KendyDigital.service.user_management.UserProfileService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final TicketRepository ticketRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final UserAvatarService userAvatarService;
    private final UserDataPortabilityService userDataPortabilityService;

    public UserProfileServiceImpl(
            UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            DepositRequestRepository depositRequestRepository,
            TicketRepository ticketRepository,
            WalletTransactionRepository walletTransactionRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            UserAvatarService userAvatarService,
            UserDataPortabilityService userDataPortabilityService) {
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.ticketRepository = ticketRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.userAvatarService = userAvatarService;
        this.userDataPortabilityService = userDataPortabilityService;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponse getProfile(Long userId) {
        return AuthUserResponse.from(getUser(userId));
    }

    @Override
    @Transactional
    public AuthUserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserAccount user = getUser(userId);
        user.setName(request.name().trim());
        user.setPhone(blankToNull(request.phone()));
        auditService.recordSystem("USER_PROFILE_UPDATED", "USER", user.getId(), null);
        return AuthUserResponse.from(user);
    }

    @Override
    @Transactional
    public AuthUserResponse uploadAvatar(Long userId, MultipartFile file) {
        UserAccount user = getUser(userId);
        String secureUrl = userAvatarService.uploadAvatar(user.getId(), String.valueOf(user.getPublicId()), file);
        user.setAvatarUrl(secureUrl);
        auditService.recordSystem("USER_AVATAR_UPDATED", "USER", user.getId(), null);
        return AuthUserResponse.from(user);
    }

    @Override
    @Transactional
    public AuthUserResponse updateLocale(Long userId, String locale) {
        UserAccount user = getUser(userId);
        user.setLocale(locale);
        return AuthUserResponse.from(user);
    }

    @Override
    @Transactional
    public AuthUserResponse changePassword(Long userId, ChangePasswordRequest request) {
        UserAccount user = getUser(userId);
        if (!user.hasPassword() || user.getPasswordHash() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Account does not have a password set. Please use set-password.");
        }
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

    @Override
    @Transactional
    public AuthUserResponse setPassword(Long userId, SetPasswordRequest request) {
        UserAccount user = getUser(userId);
        if (user.hasPassword()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Account already has a password. Use change-password instead.");
        }
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        auditService.recordSystem("USER_PASSWORD_SET", "USER", user.getId(),
                "oauth_provider=" + user.getOauthProvider());
        return AuthUserResponse.from(user);
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> exportPersonalData(Long userId) {
        return userDataPortabilityService.exportPersonalData(userId);
    }

    @Override
    @Transactional
    public Map<String, Object> deleteAccount(Long userId) {
        return userDataPortabilityService.deleteAccount(userId);
    }

    private UserAccount getUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
