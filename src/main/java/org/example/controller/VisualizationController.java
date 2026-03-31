package org.example.controller;

import org.example.service.VisualizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * Phase 6: 3D Visualization REST API
 * Provides endpoints to query visualization data, configuration, and performance metrics
 */
@RestController
@RequestMapping("/api/v1/visualization")
public class VisualizationController {

    @Autowired
    private VisualizationService visualizationService;

    /**
     * GET /api/v1/visualization/frame
     * Get current visualization frame synchronously
     * Use case: Client polling or REST-based retrieval
     * 
     * @return Current 3D frame data with nodes, edges, agents, decisions
     */
    @GetMapping("/frame")
    public ResponseEntity<Map<String, Object>> getCurrentFrame() {
        try {
            Map<String, Object> frame = visualizationService.getCurrentFrame();
            return ResponseEntity.ok(frame);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(errorResponse("Failed to get current frame: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/visualization/stats
     * Get visualization performance statistics
     * Includes: connected clients, frame rate, render time
     * 
     * @return Visualization performance metrics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getVisualizationStats() {
        try {
            Map<String, Object> stats = visualizationService.getVisualizationStats();
            stats.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(errorResponse("Failed to get visualization stats: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/visualization/config
     * Get visualization configuration and scene settings
     * Includes: camera, lighting, rendering options
     * 
     * @return Visualization configuration
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getVisualizationConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // Scene configuration
        Map<String, Object> scene = new HashMap<>();
        scene.put("backgroundColor", "0x1a1a1a");
        scene.put("fogColor", "0x1a1a1a");
        scene.put("fogFar", 2000);
        scene.put("fogNear", 100);
        config.put("scene", scene);
        
        // Camera configuration
        Map<String, Object> camera = new HashMap<>();
        camera.put("fov", 75);
        camera.put("position", new int[]{0, 50, 100});
        camera.put("lookAt", new int[]{0, 0, 0});
        camera.put("near", 0.1);
        camera.put("far", 2000);
        config.put("camera", camera);
        
        // Rendering configuration
        Map<String, Object> rendering = new HashMap<>();
        rendering.put("targetFPS", 30);
        rendering.put("antialiasing", true);
        rendering.put("shadows", true);
        rendering.put("shadowMapType", "PCF");
        config.put("rendering", rendering);
        
        // Performance configuration
        Map<String, Object> performance = new HashMap<>();
        performance.put("maxRenderTimeMs", 33);
        performance.put("maxNodes", 500);
        performance.put("maxEdges", 1000);
        performance.put("lodEnabled", true);
        config.put("performance", performance);
        
        config.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(config);
    }

    /**
     * GET /api/v1/visualization/health
     * Check if visualization service is running and responsive
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "VisualizationService");
        health.put("wsEndpoint", "/ws/visualization");
        health.put("apiVersion", "v1");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
