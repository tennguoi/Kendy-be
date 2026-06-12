package com.example.KendyDigital.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.KendyDigital.dto.notification.response.UserNotificationResponse;
import com.example.KendyDigital.model.UserAccount;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NotificationRealtimeService extends TextWebSocketHandler {
    private static final String USER_ID_ATTRIBUTE = "userId";

    private final AuthTokenService authTokenService;
    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public NotificationRealtimeService(AuthTokenService authTokenService, ObjectMapper objectMapper) {
        this.authTokenService = authTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Optional<UserAccount> user = authTokenService.resolveUser(tokenFrom(session.getUri()));
        if (user.isEmpty()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
            return;
        }

        Long userId = user.get().getId();
        session.getAttributes().put(USER_ID_ATTRIBUTE, userId);
        sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        session.sendMessage(jsonMessage(Map.of("type", "notifications.connected")));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        remove(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void publishNotification(Long userId, UserNotificationResponse notification, long unreadCount) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "type", "notification.created",
                "notification", notificationPayload(notification),
                "unreadCount", unreadCount);
        TextMessage message;
        try {
            message = jsonMessage(payload);
        } catch (IOException exception) {
            return;
        }

        sessions.removeIf(session -> !send(session, message));
    }

    private boolean send(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            return false;
        }
        try {
            session.sendMessage(message);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private void remove(WebSocketSession session) {
        Object userId = session.getAttributes().get(USER_ID_ATTRIBUTE);
        if (!(userId instanceof Long id)) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByUser.get(id);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByUser.remove(id);
            }
        }
    }

    private TextMessage jsonMessage(Object payload) throws IOException {
        return new TextMessage(objectMapper.writeValueAsString(payload));
    }

    private Map<String, Object> notificationPayload(UserNotificationResponse notification) {
        return Map.of(
                "id", notification.id(),
                "title", nullSafe(notification.title()),
                "message", nullSafe(notification.message()),
                "type", nullSafe(notification.type()),
                "actionUrl", nullSafe(notification.actionUrl()),
                "readAt", notification.readAt() == null ? "" : notification.readAt().toString(),
                "createdAt", notification.createdAt() == null ? "" : notification.createdAt().toString());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String tokenFrom(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return "";
        }

        for (String pair : uri.getQuery().split("&")) {
            int separator = pair.indexOf('=');
            String key = separator >= 0 ? pair.substring(0, separator) : pair;
            if ("token".equals(key)) {
                String value = separator >= 0 ? pair.substring(separator + 1) : "";
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }
        return "";
    }
}
