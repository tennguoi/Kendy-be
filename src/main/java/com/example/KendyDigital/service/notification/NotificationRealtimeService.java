package com.example.KendyDigital.service.notification;

import com.example.KendyDigital.dto.notification.response.UserNotificationResponse;
import com.example.KendyDigital.dto.notification.response.AdminNotificationResponse;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

public interface NotificationRealtimeService {
    void afterConnectionEstablished(WebSocketSession session) throws Exception;
    void afterConnectionClosed(WebSocketSession session, CloseStatus status);
    void handleTransportError(WebSocketSession session, Throwable exception) throws Exception;
    void publishNotification(Long userId, UserNotificationResponse notification, long unreadCount);
    void publishAdminNotification(AdminNotificationResponse notification);
}
