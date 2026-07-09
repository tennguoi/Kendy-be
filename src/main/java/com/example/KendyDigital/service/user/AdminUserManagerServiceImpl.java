package com.example.KendyDigital.service.user;

import com.example.KendyDigital.dto.user.request.AdminUserRoleUpdateRequest;
import com.example.KendyDigital.dto.user.request.AdminUserStatusUpdateRequest;
import com.example.KendyDigital.dto.user.response.AdminUserDetailResponse;
import com.example.KendyDigital.dto.user.response.AdminUserResponse;
import com.example.KendyDigital.model.deposit.DepositStatus;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.WalletTransactionRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserManagerServiceImpl  implements AdminUserManagerService{
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TicketRepository ticketRepository;
    private final AuditService auditService;
    private final UserNotificationService userNotificationService;

    public AdminUserManagerServiceImpl(UserAccountRepository userAccountRepository,
            OrderRepository orderRepository,
            DepositRequestRepository depositRequestRepository,
            WalletTransactionRepository walletTransactionRepository,
            TicketRepository ticketRepository,
            AuditService auditService,
            UserNotificationService userNotificationService) {
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.ticketRepository = ticketRepository;
        this.auditService = auditService;
        this.userNotificationService = userNotificationService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(UserStatus status) {
        return listUsers(status, null);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(UserStatus status, int page, int size) {
        List<UserAccount> users = status == null
                ? userAccountRepository.findAllByOrderByCreatedAtDesc(paged(page, size))
                : userAccountRepository.findAllByStatusOrderByCreatedAtDesc(status, paged(page, size));
        return users.stream().map(AdminUserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(UserStatus status, Integer limit) {
        return listUsers(status, 0, limit == null ? 100 : limit);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> searchUsers(String query, UserStatus status, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        List<UserAccount> users = userAccountRepository.searchAdmin(
                likePattern(normalizedQuery),
                parseLongOrNull(normalizedQuery),
                status,
                paged(page, size));
        return users.stream().map(AdminUserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> searchUsers(String query, UserStatus status, Integer limit) {
        return searchUsers(query, status, 0, limit == null ? 100 : limit);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new AdminUserDetailResponse(
                AdminUserResponse.from(user),
                orderRepository.countByUser_Id(userId),
                depositRequestRepository.countByUser_Id(userId),
                walletTransactionRepository.countByUser_Id(userId),
                ticketRepository.countByUser_Id(userId),
                depositRequestRepository.sumAmountByUserIdAndStatus(userId, DepositStatus.COMPLETED),
                walletTransactionRepository.sumAmountByUserIdAndType(userId, WalletTransactionType.PURCHASE),
                walletTransactionRepository.sumAmountByUserIdAndType(userId, WalletTransactionType.REFUND));
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long adminUserId, Long targetUserId, AdminUserStatusUpdateRequest request) {
        UserAccount user = userAccountRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setStatus(request.status());
        auditService.recordAdmin(
                adminUserId,
                "USER_STATUS_UPDATED",
                "USER",
                user.getId(),
                "status=" + request.status() + ",reason=" + blankToNull(request.reason()));
        userNotificationService.create(user.getId(), "Account status changed",
            "Your account status has been changed to " + request.status() + ". Reason: " + blankToNull(request.reason()),
            "SECURITY", "/account/security");
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse updateUserRole(Long adminUserId, Long targetUserId, AdminUserRoleUpdateRequest request) {
        if (adminUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin cannot change own role");
        }
        UserAccount adminUser = userAccountRepository.findByIdForUpdate(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (adminUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN can change user roles");
        }
        UserAccount user = userAccountRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(request.role());
        auditService.recordAdmin(
                adminUserId,
                "USER_ROLE_UPDATED",
                "USER",
                user.getId(),
                "role=" + request.role() + ",reason=" + blankToNull(request.reason()));
        userNotificationService.create(user.getId(), "Account role changed",
            "Your account role has been changed to " + request.role(),
            "SECURITY", "/account/security");
        return AdminUserResponse.from(user);
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? null : query.trim();
    }

    private String likePattern(String query) {
        return query == null ? null : "%" + query.toLowerCase(Locale.ROOT) + "%";
    }

    private Long parseLongOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
