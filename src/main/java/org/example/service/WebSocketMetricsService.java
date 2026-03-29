package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Phase 4.1: WebSocket Real-Time Metrics Broadcasting
 * Pushes live metrics to connected clients instead of polling
 * Reduces bandwidth, increases responsiveness, enables real-time notifications
 */
@Service
public class WebSocketMetricsService {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @Autowired
    private MetricsService metricsService;
    
    @Autowired
    private AlertingService alertingService;

    public WebSocketMetricsService() {
        // Broadcast metrics every 2 seconds to all connected clients
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
            this::broadcastMetrics,
            2, 2, TimeUnit.SECONDS
        );
    }

    /**
     * Register a new WebSocket session
     */
    public void registerSession(WebSocketSession session) {
        sessions.add(session);
        System.out.println("WebSocket client connected: " + session.getId());
        sendInitialData(session);
    }

    /**
     * Unregister a WebSocket session
     */
    public void unregisterSession(WebSocketSession session) {
        sessions.remove(session);
        System.out.println("WebSocket client disconnected: " + session.getId());
    }

    /**
     * Broadcast current metrics to all connected clients
     */
    public void broadcastMetrics() {
        if (sessions.isEmpty()) return;

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "metrics_update");
            data.put("timestamp", System.currentTimeMillis());
            data.put("health", metricsService.getSystemHealth());
            data.put("stats", metricsService.getGenerationStats());
            data.put("alerts", alertingService.getActiveAlerts());

            String json = objectMapper.writeValueAsString(data);
            TextMessage message = new TextMessage(json);

            // Send to all connected clients
            for (WebSocketSession session : new ArrayList<>(sessions)) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        unregisterSession(session);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error broadcasting metrics: " + e.getMessage());
        }
    }

    /**
     * Send initial data when client connects
     */
    private void sendInitialData(WebSocketSession session) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "initial_data");
            data.put("health", metricsService.getSystemHealth());
            data.put("stats", metricsService.getGenerationStats());
            data.put("alerts", alertingService.getAlertStats());
            data.put("message", "Connected to SupremeAI Monitoring");

            String json = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            System.err.println("Error sending initial data: " + e.getMessage());
        }
    }

    /**
     * Broadcast alert immediately when triggered
     */
    public void broadcastAlert(AlertingService.Alert alert) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "alert");
            data.put("alert", alert);
            data.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(data);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : new ArrayList<>(sessions)) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        unregisterSession(session);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error broadcasting alert: " + e.getMessage());
        }
    }

    public int getConnectedClients() {
        return sessions.size();
    }
}
