package com.example.KendyDigital.service.notification;

import com.example.KendyDigital.dto.notification.request.UserNotificationSettingsRequest;
import com.example.KendyDigital.dto.notification.response.UserNotificationResponse;
import com.example.KendyDigital.dto.notification.response.UserNotificationSettingsResponse;
import java.util.List;

public interface UserNotificationService {
    List<UserNotificationResponse> list(Long userId, int page, int size);
    long unreadCount(Long userId);
    UserNotificationResponse markRead(Long userId, Long id);
    List<UserNotificationResponse> bulkRead(Long userId, List<Long> ids);
    UserNotificationSettingsResponse settings(Long userId);
    UserNotificationSettingsResponse updateSettings(Long userId, UserNotificationSettingsRequest request);
    void create(Long userId, String title, String message, String type, String actionUrl);
    void createLocalized(Long userId, String titleKey, String messageKey, Object[] titleArgs, Object[] messageArgs, String type, String actionUrl);
}
