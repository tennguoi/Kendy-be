package com.example.KendyDigital.config;

import com.example.KendyDigital.service.notification.impl.NotificationRealtimeServiceImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final AppSecurityProperties securityProperties;
    private final NotificationRealtimeServiceImpl notificationRealtimeService;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(AppSecurityProperties securityProperties,
            NotificationRealtimeServiceImpl notificationRealtimeService,
            WebSocketHandshakeInterceptor handshakeInterceptor) {
        this.securityProperties = securityProperties;
        this.notificationRealtimeService = notificationRealtimeService;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationRealtimeService, "/ws/notifications")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(securityProperties.getCorsAllowedOrigins().toArray(String[]::new));
    }
}
