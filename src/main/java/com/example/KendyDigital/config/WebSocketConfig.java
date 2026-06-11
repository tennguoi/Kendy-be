package com.example.KendyDigital.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.example.KendyDigital.service.NotificationRealtimeService;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final AppSecurityProperties securityProperties;
    private final NotificationRealtimeService notificationRealtimeService;

    public WebSocketConfig(AppSecurityProperties securityProperties,
            NotificationRealtimeService notificationRealtimeService) {
        this.securityProperties = securityProperties;
        this.notificationRealtimeService = notificationRealtimeService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationRealtimeService, "/ws/notifications")
                .setAllowedOrigins(securityProperties.getCorsAllowedOrigins().toArray(String[]::new));
    }
}
