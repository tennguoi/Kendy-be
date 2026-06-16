package com.example.KendyDigital.service.notification;

import com.example.KendyDigital.dto.notification.request.UserNotificationSettingsRequest;
import com.example.KendyDigital.dto.notification.response.UserNotificationResponse;
import com.example.KendyDigital.dto.notification.response.UserNotificationSettingsResponse;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserNotification;
import com.example.KendyDigital.model.user.UserNotificationSettings;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserNotificationRepository;
import com.example.KendyDigital.repository.UserNotificationSettingsRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserNotificationServiceImpl  implements UserNotificationService{
    private final UserNotificationRepository userNotificationRepository;
    private final UserNotificationSettingsRepository settingsRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmailNotificationService emailNotificationService;
    private final NotificationRealtimeService notificationRealtimeService;

    public UserNotificationServiceImpl(UserNotificationRepository userNotificationRepository,
            UserNotificationSettingsRepository settingsRepository,
            UserAccountRepository userAccountRepository,
            EmailNotificationService emailNotificationService,
            NotificationRealtimeService notificationRealtimeService) {
        this.userNotificationRepository = userNotificationRepository;
        this.settingsRepository = settingsRepository;
        this.userAccountRepository = userAccountRepository;
        this.emailNotificationService = emailNotificationService;
        this.notificationRealtimeService = notificationRealtimeService;
    }

    @Transactional(readOnly = true)
    public List<UserNotificationResponse> list(Long userId, int page, int size) {
        return userNotificationRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(UserNotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return userNotificationRepository.countByUser_IdAndReadAtIsNull(userId);
    }

    @Transactional
    public UserNotificationResponse markRead(Long userId, Long id) {
        UserNotification notification = userNotificationRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.markRead();
        return UserNotificationResponse.from(notification);
    }

    @Transactional
    public List<UserNotificationResponse> bulkRead(Long userId, List<Long> ids) {
        List<UserNotification> notifications = userNotificationRepository.findAllById(ids);
        return notifications.stream()
                .peek(n -> {
                    if (!n.getUser().getId().equals(userId)) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
                    }
                    n.markRead();
                })
                .map(UserNotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserNotificationSettingsResponse settings(Long userId) {
        return UserNotificationSettingsResponse.from(requireSettings(userId));
    }

    @Transactional
    public UserNotificationSettingsResponse updateSettings(Long userId, UserNotificationSettingsRequest request) {
        UserNotificationSettings settings = requireSettings(userId);
        settings.update(
                request.orderUpdates(),
                request.depositUpdates(),
                request.ticketUpdates(),
                request.walletUpdates(),
                request.securityUpdates(),
                request.emailNotifications());
        return UserNotificationSettingsResponse.from(settings);
    }

    @Transactional
    public void create(Long userId, String title, String message, String type, String actionUrl) {
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user != null) {
            UserNotification notification = userNotificationRepository.save(
                    new UserNotification(user, title, message, type, actionUrl));
            notificationRealtimeService.publishNotification(
                    userId,
                    UserNotificationResponse.from(notification),
                    unreadCount(userId));
            if (shouldSendEmail(userId, type)) {
                emailNotificationService.sendUserNotification(user, notification.getTitle(),
                        notification.getMessage(), notification.getActionUrl());
            }
        }
    }

    private boolean shouldSendEmail(Long userId, String type) {
        UserNotificationSettings settings = requireSettings(userId);
        if (!settings.isEmailNotifications()) {
            return false;
        }
        if ("ORDER".equalsIgnoreCase(type)) {
            return settings.isOrderUpdates();
        }
        if ("DEPOSIT".equalsIgnoreCase(type)) {
            return settings.isDepositUpdates();
        }
        if ("TICKET".equalsIgnoreCase(type)) {
            return settings.isTicketUpdates();
        }
        if ("WALLET".equalsIgnoreCase(type)) {
            return settings.isWalletUpdates();
        }
        if ("SECURITY".equalsIgnoreCase(type)) {
            return settings.isSecurityUpdates();
        }
        return true;
    }

    private UserNotificationSettings requireSettings(Long userId) {
        return settingsRepository.findById(userId)
                .orElseGet(() -> {
                    UserAccount user = userAccountRepository.findById(userId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                    return settingsRepository.save(new UserNotificationSettings(user));
                });
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }
}
