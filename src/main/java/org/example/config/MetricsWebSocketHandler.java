package org.example.config;

import org.example.service.WebSocketMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket Handler for real-time metrics streaming
 */
@Component
public class MetricsWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private WebSocketMetricsService webSocketMetricsService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket: Client connected - " + session.getId());
        webSocketMetricsService.registerSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Client can send commands, e.g., {"action": "request_snapshot"}
        String payload = message.getPayload();
        if (payload.contains("snapshot")) {
            // Send immediate snapshot
            webSocketMetricsService.broadcastMetrics();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("WebSocket: Client disconnected - " + session.getId());
        webSocketMetricsService.unregisterSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket error: " + exception.getMessage());
        webSocketMetricsService.unregisterSession(session);
    }
}
