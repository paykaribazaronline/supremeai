package com.supremeai.websocket;

import com.google.gson.Gson;
import com.supremeai.event.AgentStatusEvent;
import com.supremeai.event.MetricUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AdminWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminWebSocketHandler.class);

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

    /**
     * Periodically send visualization frames for the 3D dashboard
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 2000)
    public void broadcastVisualizationFrame() {
        if (sessions.isEmpty()) return;

        Map<String, Object> frame = Map.of(
                "type", "frame_update",
                "timestamp", System.currentTimeMillis(),
                "agents", List.of(
                        Map.of("id", "agent-1", "name", "Orchestrator", "position", List.of(0, 0, 0), "status", "IDLE", "color", 0x00ff00, "scale", 2.0, "votingProgress", 0),
                        Map.of("id", "agent-2", "name", "Coder", "position", List.of(30, 20, -10), "status", "WORKING", "color", 0x00aaff, "scale", 1.5, "votingProgress", 45),
                        Map.of("id", "agent-3", "name", "Tester", "position", List.of(-30, 20, -10), "status", "WAITING", "color", 0xffaa00, "scale", 1.5, "votingProgress", 0)
                ),
                "buildFlow", Map.of(
                        "nodes", List.of(
                                Map.of("id", "node-1", "label", "Requirement", "position", List.of(0, 80, 0), "color", 0xffffff, "scale", 3.0),
                                Map.of("id", "node-2", "label", "Design", "position", List.of(50, 60, -30), "color", 0xaaaaff, "scale", 2.5),
                                Map.of("id", "node-3", "label", "Build", "position", List.of(-50, 60, -30), "color", 0xaffaff, "scale", 2.5)
                        ),
                        "edges", List.of(
                                Map.of("from", "node-1", "to", "node-2", "color", 0x4444ff, "thickness", 1.0, "dashed", false),
                                Map.of("from", "node-1", "to", "node-3", "color", 0x44ff44, "thickness", 1.0, "dashed", false)
                        )
                )
        );

        broadcastToAll(frame);
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
