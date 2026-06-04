package com.supremeai.controller;

import com.supremeai.service.SimulatorSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.time.Instant;
import java.util.Map;

/**
 * Simulator WebSocket Controller - Remote control of simulator sessions.
 *
 * Uses STOMP over WebSocket (consistent with existing WebSocketConfig).
 *
 * Destinations:
 * - SEND to: /app/simulator/control/{sessionId}   (admin → simulator)
 * - SEND to: /app/simulator/event/{sessionId}     (simulator → backend)
 * - SUBSCRIBE to: /topic/simulator/{sessionId}    (simulator receives commands)
 * - SUBSCRIBE to: /topic/simulator/events         (admin receives events)
 */
@Controller
public class SimulatorWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorWebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SimulatorSessionService sessionService;

    /**
     * Admin UI sends a control command to a simulator session.
     *
     * Client sends STOMP message to: /app/simulator/control/{sessionId}
     * Payload: { "type": "tap", "x": 100, "y": 200 } or { "type": "input", "selector": "#email", "text": "test" }
     *
     * This relays the command to the simulator frontend via topic.
     */
    @MessageMapping("/simulator/control/{sessionId}")
    public void handleControlCommand(
            @DestinationVariable String sessionId,
            @Payload Map<String, Object> command) {

        // Validate session exists
        if (!sessionService.isActive(sessionId)) {
            logger.warn("[WS] Control command for inactive session: {}", sessionId);
            return;
        }

        // Enrich command with metadata
        command.put("_sessionId", sessionId);
        command.put("_timestamp", Instant.now().toString());

        // Forward to simulator frontend (the page running in iframe or separate tab)
        messagingTemplate.convertAndSend("/topic/simulator/" + sessionId, Map.of(
                "type", "command",
                "command", command
        ));

        logger.debug("[WS] Relayed control command to session {}: {}", sessionId, command.get("type"));
    }

    /**
     * Simulator frontend sends event/heartbeat to backend.
     *
     * Client sends to: /app/simulator/event/{sessionId}
     * Payload: { "type": "heartbeat" } or { "type": "log", "level": "ERROR", "message": "..." }
     */
    @MessageMapping("/simulator/event/{sessionId}")
    public void handleSimulatorEvent(
            @DestinationVariable String sessionId,
            @Payload Map<String, Object> event) {

        String eventType = (String) event.getOrDefault("type", "unknown");
        logger.debug("[WS] Event from session {}: {}", sessionId, eventType);

        // Update session heartbeat
        sessionService.refreshHeartbeat(sessionId);

        // Broadcast to admin UI (for live monitoring)
        Map<String, Object> adminEvent = Map.of(
                "sessionId", sessionId,
                "event", event,
                "timestamp", Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/simulator/events", adminEvent);

        // Handle specific events
        if ("screenshot".equals(eventType)) {
            // Could trigger screenshot capture service
        } else if ("log".equals(eventType)) {
            // Store logs in Firestore for later analysis
        }
    }

    /**
     * Admin UI requests screenshot of active simulator.
     * Sends command, simulator responds via event with base64 image.
     */
    @MessageMapping("/simulator/admin/capture")
    public void captureScreenshot(@Payload Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        if (sessionId == null) return;

        // Send command to simulator
        Map<String, Object> cmd = Map.of("type", "screenshot", "format", "png");
        messagingTemplate.convertAndSend("/topic/simulator/" + sessionId, Map.of(
                "type", "command",
                "command", cmd
        ));
    }
}
