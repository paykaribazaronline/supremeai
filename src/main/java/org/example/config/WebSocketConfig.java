package org.example.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket Configuration for real-time metrics streaming and 3D visualization
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MetricsWebSocketHandler metricsWebSocketHandler;
    
    @Autowired
    private VisualizationWebSocketHandler visualizationWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Phase 4.1: Metrics WebSocket endpoint
        registry.addHandler(metricsWebSocketHandler, "/ws/metrics")
                .setAllowedOrigins("*")
                .withSockJS(); // Fallback for browsers without WebSocket support

        // Phase 6: 3D Visualization WebSocket endpoint
        registry.addHandler(visualizationWebSocketHandler, "/ws/visualization")
                .setAllowedOrigins("*")
                .withSockJS();

        System.out.println("✓ WebSocket handler registered at /ws/metrics");
        System.out.println("✓ WebSocket handler registered at /ws/visualization (Phase 6)");
    }
}
