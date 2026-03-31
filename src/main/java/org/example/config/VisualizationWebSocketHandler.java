package org.example.config;

import org.example.service.VisualizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 6: WebSocket Handler for 3D Visualization
 * Streams real-time 3D data to frontend at 30 FPS (~33ms per frame)
 * Handles client connections, commands, and disconnections
 */
@Component
public class VisualizationWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private VisualizationService visualizationService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handle new WebSocket connection
     * Sends initial scene configuration and starts receiving frames
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("✓ 3D Visualization WebSocket: Client connected - " + session.getId());
        visualizationService.registerVisualizationSession(session);
        
        // Send connection confirmation
        Map<String, Object> confirmation = new HashMap<>();
        confirmation.put("type", "connection_acknowledged");
        confirmation.put("clientId", session.getId());
        confirmation.put("timestamp", System.currentTimeMillis());
        
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmation)));
    }

    /**
     * Handle messages from client
     * Supports: scene_settings, camera_update, view_mode, pause/resume
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        try {
            Map<String, Object> command = objectMapper.readValue(payload, Map.class);
            String action = (String) command.get("action");
            
            if (action == null) return;
            
            switch (action) {
                case "request_frame":
                    // Client requesting immediate frame
                    Map<String, Object> frame = visualizationService.getCurrentFrame();
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(frame)));
                    break;
                    
                case "scene_settings":
                    // Client requesting scene configuration
                    Map<String, Object> sceneSettings = buildSceneSettings();
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(sceneSettings)));
                    break;
                    
                case "stats":
                    // Client requesting visualization stats
                    Map<String, Object> stats = visualizationService.getVisualizationStats();
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(stats)));
                    break;
                    
                default:
                    System.out.println("Unknown visualization action: " + action);
            }
        } catch (Exception e) {
            System.err.println("Error handling visualization command: " + e.getMessage());
        }
    }

    /**
     * Handle client disconnection
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("✗ 3D Visualization WebSocket: Client disconnected - " + session.getId());
        visualizationService.unregisterVisualizationSession(session);
    }

    /**
     * Handle WebSocket transport errors
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("✗ 3D Visualization WebSocket error (" + session.getId() + "): " + exception.getMessage());
        visualizationService.unregisterVisualizationSession(session);
    }

    /**
     * Build default scene settings for client
     */
    private Map<String, Object> buildSceneSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("type", "scene_settings");
        
        Map<String, Object> rendering = new HashMap<>();
        rendering.put("targetFPS", 30);
        rendering.put("maxFrameTime", 33);
        rendering.put("antialiasing", true);
        rendering.put("shadows", true);
        
        Map<String, Object> performance = new HashMap<>();
        performance.put("enableOptimizations", true);
        performance.put("maxNodes", 500);
        performance.put("maxEdges", 1000);
        performance.put("lodDistances", new HashMap<String, Integer>() {{
            put("detail", 100);
            put("medium", 300);
            put("low", 1000);
        }});
        
        settings.put("rendering", rendering);
        settings.put("performance", performance);
        
        return settings;
    }
}
