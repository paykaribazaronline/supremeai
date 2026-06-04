package com.supremeai.websocket;

import com.supremeai.service.SimulatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simulator WebSocket Handler - Bi-directional remote control for simulator sessions.
 *
 * Message Types (JSON):
 * - tap: { "type": "tap", "x": 150, "y": 300 }
 * - swipe: { "type": "swipe", "fromX": 0, "fromY": 800, "toX": 0, "toY": 0 }
 * - input: { "type": "input", "selector": "#username", "text": "test" }
 * - scroll: { "type": "scroll", "direction": "down", "amount": 300 }
 * - screenshot: { "type": "screenshot" } → returns base64 image
 * - log: { "type": "log", "level": "INFO", "message": "..." }
 * - heartbeat: { "type": "heartbeat" }
 * - terminate: { "type": "terminate", "reason": "..." }
 */
@Component
public class SimulatorWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorWebSocketHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, SimulatorSessionState> sessionStates = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);
    private final AtomicLong messageCounter = new AtomicLong(0);

    @Autowired
    private SimulatorService simulatorService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = extractSessionId(session);
        sessionStates.put(sessionId, new SimulatorSessionState(sessionId));
        activeSessions.put(sessionId, session);

        logger.info("[SIM_WS] Session connected: {} (total active: {})",
            sessionId, activeSessions.size());

        // Start heartbeat for this session
        heartbeatScheduler.scheduleAtFixedRate(
            () -> sendHeartbeat(sessionId),
            5, 5, TimeUnit.SECONDS
        );

        // Send welcome message
        sendToSession(sessionId, Map.of(
            "type", "connected",
            "sessionId", sessionId,
            "message", "Simulator session active"
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = extractSessionId(session);
        long msgNum = messageCounter.incrementAndGet();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = mapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.getOrDefault("type", "unknown");

            logger.debug("[SIM_WS] Message #{} type={} session={}", msgNum, type, sessionId);

            switch (type) {
                case "tap" -> handleTap(sessionId, payload);
                case "swipe" -> handleSwipe(sessionId, payload);
                case "input" -> handleInput(sessionId, payload);
                case "scroll" -> handleScroll(sessionId, payload);
                case "screenshot" -> handleScreenshotRequest(sessionId);
                case "heartbeat" -> handleHeartbeat(sessionId);
                case "terminate" -> handleTerminate(sessionId, payload);
                default -> sendToSession(sessionId, Map.of(
                    "type", "error",
                    "message", "Unknown command type: " + type
                ));
            }

            // Update last activity
            SimulatorSessionState state = sessionStates.get(sessionId);
            if (state != null) {
                state.lastActivity = System.currentTimeMillis();
            }

        } catch (Exception e) {
            logger.error("[SIM_WS] Failed to process message #{} from session {}: {}",
                msgNum, sessionId, e.getMessage());
            sendToSession(sessionId, Map.of(
                "type", "error",
                "message", "Failed to process: " + e.getMessage()
            ));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = extractSessionId(session);
        activeSessions.remove(sessionId);
        SimulatorSessionState state = sessionStates.remove(sessionId);

        logger.info("[SIM_WS] Session disconnected: {} status={} (active: {})",
            sessionId, status, activeSessions.size());

        if (state != null) {
            state.active = false;
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = extractSessionId(session);
        logger.error("[SIM_WS] Transport error for session {}: {}", sessionId, exception.getMessage());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Message handlers
    // ──────────────────────────────────────────────────────────────────────

    private void handleTap(String sessionId, Map<String, Object> payload) {
        Number x = (Number) payload.getOrDefault("x", 0);
        Number y = (Number) payload.getOrDefault("y", 0);

        SimulatorSessionState state = sessionStates.get(sessionId);
        if (state != null) state.tapCount++;

        logger.debug("[SIM_WS] Tap at ({}, {}) session={}", x, y, sessionId);

        // Echo back as event confirmation
        sendToSession(sessionId, Map.of(
            "type", "tap_ack",
            "x", x.intValue(),
            "y", y.intValue(),
            "tapCount", state != null ? state.tapCount : 0
        ));
    }

    private void handleSwipe(String sessionId, Map<String, Object> payload) {
        Number fromX = (Number) payload.getOrDefault("fromX", 0);
        Number fromY = (Number) payload.getOrDefault("fromY", 0);
        Number toX = (Number) payload.getOrDefault("toX", 0);
        Number toY = (Number) payload.getOrDefault("toY", 0);

        logger.debug("[SIM_WS] Swipe from ({},{}) to ({},{}) session={}",
            fromX, fromY, toX, toY, sessionId);

        sendToSession(sessionId, Map.of(
            "type", "swipe_ack",
            "fromX", fromX.intValue(), "fromY", fromY.intValue(),
            "toX", toX.intValue(), "toY", toY.intValue()
        ));
    }

    private void handleInput(String sessionId, Map<String, Object> payload) {
        String selector = (String) payload.getOrDefault("selector", "");
        String text = (String) payload.getOrDefault("text", "");

        logger.debug("[SIM_WS] Input '{}' into '{}' session={}", text, selector, sessionId);

        sendToSession(sessionId, Map.of(
            "type", "input_ack",
            "selector", selector,
            "length", text.length()
        ));
    }

    private void handleScroll(String sessionId, Map<String, Object> payload) {
        String direction = (String) payload.getOrDefault("direction", "down");
        Number amount = (Number) payload.getOrDefault("amount", 100);

        logger.debug("[SIM_WS] Scroll {} {}px session={}", direction, amount, sessionId);

        sendToSession(sessionId, Map.of(
            "type", "scroll_ack",
            "direction", direction,
            "amount", amount.intValue()
        ));
    }

    private void handleScreenshotRequest(String sessionId) {
        logger.debug("[SIM_WS] Screenshot requested for session={}", sessionId);

        // In production, would capture actual screenshot via Playwright
        sendToSession(sessionId, Map.of(
            "type", "screenshot",
            "data", "[screenshot_placeholder_base64]",
            "format", "png",
            "width", 1080,
            "height", 2340
        ));
    }

    private void handleHeartbeat(String sessionId) {
        SimulatorSessionState state = sessionStates.get(sessionId);
        if (state != null) {
            state.lastHeartbeat = System.currentTimeMillis();
        }
        sendToSession(sessionId, Map.of("type", "heartbeat_ack"));
    }

    private void handleTerminate(String sessionId, Map<String, Object> payload) {
        String reason = (String) payload.getOrDefault("reason", "user_request");
        logger.info("[SIM_WS] Terminate session {}: {}", sessionId, reason);

        sendToSession(sessionId, Map.of(
            "type", "terminated",
            "reason", reason
        ));

        closeSession(sessionId);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Utilities
    // ──────────────────────────────────────────────────────────────────────

    private String extractSessionId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        // Path: /ws/simulator/{sessionId}
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : session.getId();
    }

    private void sendToSession(String sessionId, Map<String, Object> message) {
        WebSocketSession wsSession = activeSessions.get(sessionId);
        if (wsSession != null && wsSession.isOpen()) {
            try {
                String json = mapper.writeValueAsString(message);
                wsSession.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                logger.warn("[SIM_WS] Failed to send to session {}: {}", sessionId, e.getMessage());
            }
        }
    }

    private void sendHeartbeat(String sessionId) {
        WebSocketSession wsSession = activeSessions.get(sessionId);
        if (wsSession != null && wsSession.isOpen()) {
            sendToSession(sessionId, Map.of("type", "heartbeat"));
        }
    }

    private void closeSession(String sessionId) {
        WebSocketSession wsSession = activeSessions.remove(sessionId);
        if (wsSession != null) {
            try {
                wsSession.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                logger.warn("[SIM_WS] Error closing session {}: {}", sessionId, e.getMessage());
            }
        }
        SimulatorSessionState state = sessionStates.remove(sessionId);
        if (state != null) state.active = false;
    }

    private void broadcastLog(String level, String message) {
        Map<String, Object> logMsg = Map.of(
            "type", "log",
            "level", level,
            "message", message,
            "timestamp", System.currentTimeMillis()
        );
        activeSessions.keySet().forEach(sid -> sendToSession(sid, logMsg));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Session state
    // ──────────────────────────────────────────────────────────────────────

    private static class SimulatorSessionState {
        final String sessionId;
        volatile boolean active = true;
        volatile long lastActivity = System.currentTimeMillis();
        volatile long lastHeartbeat = System.currentTimeMillis();
        volatile int tapCount = 0;

        SimulatorSessionState(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}
