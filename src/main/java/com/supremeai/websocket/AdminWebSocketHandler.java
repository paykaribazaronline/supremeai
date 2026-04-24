package com.supremeai.websocket;

import com.google.gson.Gson;
import com.supremeai.event.AgentStatusEvent;
import com.supremeai.event.MetricUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class AdminWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Gson gson = new Gson();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connection established: {}", session.getId());
        sendDashboardData(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket connection closed: {}", session.getId());
    }

    @EventListener
    public void onAgentStatusChange(AgentStatusEvent event) {
        broadcastToAll(Map.of(
                "type", "AGENT_UPDATE",
                "agentId", event.getAgentId(),
                "status", event.getNewStatus(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    @EventListener
    public void onMetricUpdate(MetricUpdateEvent event) {
        broadcastToAll(Map.of(
                "type", "METRIC_UPDATE",
                "metric", event.getMetricName(),
                "value", event.getValue(),
                "threshold", event.getThreshold()
        ));
    }

    private void sendDashboardData(WebSocketSession session) {
        try {
            String json = gson.toJson(Map.of(
                    "type", "INITIAL_DATA",
                    "timestamp", System.currentTimeMillis()
            ));
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send initial data", e);
        }
    }

    private void broadcastToAll(Map<String, Object> message) {
        String json = gson.toJson(message);
        // Create a snapshot to avoid ConcurrentModificationException
        List<WebSocketSession> snapshot = sessions.stream().toList();
        snapshot.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                log.warn("Failed to send message to session: {}", session.getId(), e);
                sessions.remove(session);
            }
        });
    }
}
