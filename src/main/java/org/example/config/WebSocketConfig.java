package org.example.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket Configuration for real-time metrics streaming
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MetricsWebSocketHandler metricsWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(metricsWebSocketHandler, "/ws/metrics")
                .setAllowedOrigins("*")
                .withSockJS(); // Fallback for browsers without WebSocket support

        System.out.println("✓ WebSocket handler registered at /ws/metrics");
    }
}
