package com.example.KendyDigital.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.UserNotificationResponse;
import com.example.KendyDigital.dto.UserNotificationSettingsRequest;
import com.example.KendyDigital.dto.UserNotificationSettingsResponse;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserNotification;
import com.example.KendyDigital.model.UserNotificationSettings;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserNotificationRepository;
import com.example.KendyDigital.repository.UserNotificationSettingsRepository;

@Service
public class UserNotificationService {
    private final UserNotificationRepository userNotificationRepository;
    private final UserNotificationSettingsRepository settingsRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmailNotificationService emailNotificationService;

    public UserNotificationService(UserNotificationRepository userNotificationRepository,
            UserNotificationSettingsRepository settingsRepository,
            UserAccountRepository userAccountRepository,
            EmailNotificationService emailNotificationService) {
        this.userNotificationRepository = userNotificationRepository;
        this.settingsRepository = settingsRepository;
        this.userAccountRepository = userAccountRepository;
        this.emailNotificationService = emailNotificationService;
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
        return ids.stream()
                .map(id -> markRead(userId, id))
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
