package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Phase 6: 3D Real-Time Visualization Service
 * Enhanced with Phase 9: Cost Galaxy Framework
 * Generates real-time 3D visualization data for decision flows, agent coordination, and multi-cloud costs.
 */
@Service
public class VisualizationService {
    
    private final Set<WebSocketSession> visualizationSessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private DeltaCostAgent deltaCostAgent;

    public VisualizationService() {
        // Scheduler for 3D data generation (30 FPS = 33ms updates)
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
            this::broadcastVisualizationData,
            33, 33, TimeUnit.MILLISECONDS
        );
    }

    /**
     * Register a new visualization session
     */
    public void registerVisualizationSession(WebSocketSession session) {
        visualizationSessions.add(session);
        System.out.println("✓ 3D Visualization client connected: " + session.getId());
        sendInitialVisualizationData(session);
    }

    /**
     * Unregister a visualization session
     */
    public void unregisterVisualizationSession(WebSocketSession session) {
        visualizationSessions.remove(session);
        System.out.println("✗ 3D Visualization client disconnected: " + session.getId());
    }

    /**
     * Send initial 3D scene setup (graph structure, camera, lighting)
     */
    private void sendInitialVisualizationData(WebSocketSession session) {
        try {
            Map<String, Object> sceneData = new HashMap<>();
            sceneData.put("type", "scene_init");
            sceneData.put("timestamp", System.currentTimeMillis());
            
            // Scene configuration
            Map<String, Object> scene = new HashMap<>();
            scene.put("backgroundColor", 0x050505); // Deeper space background
            scene.put("fogColor", 0x050505);
            scene.put("fogFar", 3000);
            
            // Camera setup
            Map<String, Object> camera = new HashMap<>();
            camera.put("fov", 75);
            camera.put("position", new int[]{0, 100, 300}); // Zoomed out for Galaxy view
            camera.put("lookAt", new int[]{0, 0, 0});
            
            // Lighting setup
            List<Map<String, Object>> lights = new ArrayList<>();
            Map<String, Object> ambientLight = new HashMap<>();
            ambientLight.put("type", "ambient");
            ambientLight.put("color", 0xffffff);
            ambientLight.put("intensity", 0.4f);
            lights.add(ambientLight);
            
            Map<String, Object> pointLight = new HashMap<>();
            pointLight.put("type", "point");
            pointLight.put("color", 0xffaa00); // Solar glow
            pointLight.put("intensity", 1.5f);
            pointLight.put("position", new int[]{0, 0, 0});
            lights.add(pointLight);
            
            sceneData.put("scene", scene);
            sceneData.put("camera", camera);
            sceneData.put("lights", lights);
            
            String json = objectMapper.writeValueAsString(sceneData);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            unregisterVisualizationSession(session);
        }
    }

    /**
     * Broadcast real-time 3D visualization data (30 FPS)
     */
    public void broadcastVisualizationData() {
        if (visualizationSessions.isEmpty()) return;

        try {
            Map<String, Object> frame = generateVisualizationFrame();
            String json = objectMapper.writeValueAsString(frame);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : new ArrayList<>(visualizationSessions)) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        unregisterVisualizationSession(session);
                    }
                }
            }
        } catch (Exception e) {
            // Log error but keep thread alive
        }
    }

    /**
     * Generate a single frame of 3D visualization data
     * Includes the new "Cost Galaxy" logic
     */
    private Map<String, Object> generateVisualizationFrame() {
        Map<String, Object> frame = new HashMap<>();
        frame.put("type", "frame_update");
        frame.put("timestamp", System.currentTimeMillis());
        
        // 1. BuildFlow (Standard Phase 6)
        frame.put("buildFlow", new HashMap<String, Object>() {{
            put("nodes", generateBuildFlowNodes());
            put("edges", generateBuildFlowEdges());
        }});
        
        // 2. Agents (Standard Phase 6)
        frame.put("agents", generateAgentNodes());
        
        // 3. Decisions (Standard Phase 6)
        frame.put("decisions", generateDecisionIndicators());

        // 4. Phase 9: COST GALAXY (New Integration)
        frame.put("costGalaxy", generateCostGalaxy());
        
        frame.put("renderHint", new HashMap<String, Object>() {{
            put("targetFPS", 60);
            put("showGalaxy", true);
        }});
        
        return frame;
    }

    /**
     * NEW: Phase 9 Cost Galaxy Framework
     * Visualizes multi-cloud spend as a celestial system
     */
    private Map<String, Object> generateCostGalaxy() {
        Map<String, Object> galaxy = new HashMap<>();
        List<Map<String, Object>> celestialBodies = new ArrayList<>();
        List<Map<String, Object>> orbitalPaths = new ArrayList<>();

        Map<String, Object> costData = deltaCostAgent.trackCosts();
        double totalSpend = (double) costData.getOrDefault("total_monthly_spend", 0.0);

        // Center Sun: Total Project Spend
        celestialBodies.add(createCelestialBody("total_spend", "TOTAL SPEND: $" + totalSpend, 
                           0, 0, 0, 0xffcc00, 10.0f, "STAR"));

        // Cloud Planets: GCP, AWS, Azure
        @SuppressWarnings("unchecked")
        Map<String, Object> breakdown = (Map<String, Object>) costData.get("cloud_breakdown");
        if (breakdown != null) {
            int index = 0;
            for (String cloud : breakdown.keySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metrics = (Map<String, Object>) breakdown.get(cloud);
                double total = (double) metrics.get("total");
                double health = (double) metrics.get("health_score");
                
                // Orbital calculation
                double angle = (2 * Math.PI * index) / 3;
                double distance = 150 + (total / 10); // Distance based on cost
                double x = Math.cos(angle) * distance;
                double z = Math.sin(angle) * distance;

                int color = health > 90 ? 0x00ff88 : 0xff3300; // Efficiency color
                
                celestialBodies.add(createCelestialBody("cloud_" + cloud, cloud, 
                                   x, 0, z, color, 5.0f, "PLANET"));
                
                // Add orbit line
                orbitalPaths.add(createOrbit("orbit_" + cloud, distance, 0x333333));
                
                // Add Service Moons (Compute, Storage, Network)
                addServiceMoons(celestialBodies, cloud, x, z, metrics);
                
                index++;
            }
        }

        galaxy.put("bodies", celestialBodies);
        galaxy.put("orbits", orbitalPaths);
        return galaxy;
    }

    private void addServiceMoons(List<Map<String, Object>> bodies, String cloud, double px, double pz, Map<String, Object> metrics) {
        String[] services = {"compute_cost", "storage_cost", "network_cost"};
        int[] colors = {0x00aaff, 0xaa00ff, 0x00ffaa};
        
        for (int i = 0; i < services.length; i++) {
            double cost = (double) metrics.get(services[i]);
            double moonAngle = (2 * Math.PI * i) / 3 + (System.currentTimeMillis() / 1000.0);
            double moonDist = 20 + (cost / 5);
            
            double mx = px + Math.cos(moonAngle) * moonDist;
            double mz = pz + Math.sin(moonAngle) * moonDist;
            
            bodies.add(createCelestialBody(cloud + "_" + services[i], services[i], 
                               mx, 10, mz, colors[i], 1.5f, "MOON"));
        }
    }

    private Map<String, Object> createCelestialBody(String id, String name, double x, double y, double z, int color, float size, String type) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        body.put("name", name);
        body.put("position", new double[]{x, y, z});
        body.put("color", color);
        body.put("size", size);
        body.put("type", type);
        body.put("emissive", type.equals("STAR") ? 1.0 : 0.2);
        return body;
    }

    private Map<String, Object> createOrbit(String id, double radius, int color) {
        Map<String, Object> orbit = new HashMap<>();
        orbit.put("id", id);
        orbit.put("radius", radius);
        orbit.put("color", color);
        orbit.put("opacity", 0.3);
        return orbit;
    }

    /**
     * Standard Phase 6 Node Generation (Preserved)
     */
    private List<Map<String, Object>> generateBuildFlowNodes() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(createNode("root", "Project Root", 0, 50, 0, 0x00aa00, 3.0f));
        String[] services = {"AuthService", "APIService", "StorageService", "NotificationService"};
        for (int i = 0; i < services.length; i++) {
            double angle = (2 * Math.PI * i) / services.length;
            double x = Math.cos(angle) * 60;
            double z = Math.sin(angle) * 60;
            nodes.add(createNode("service_" + i, services[i], x, 70, z, 0x0088ff, 2.0f));
        }
        return nodes;
    }

    private List<Map<String, Object>> generateBuildFlowEdges() {
        List<Map<String, Object>> edges = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Map<String, Object> edge = new HashMap<>();
            edge.put("from", "root");
            edge.put("to", "service_" + i);
            edge.put("color", 0x00aa00);
            edge.put("thickness", 2);
            edges.add(edge);
        }
        return edges;
    }

    private List<Map<String, Object>> generateAgentNodes() {
        List<Map<String, Object>> agentNodes = new ArrayList<>();
        String[] agentNames = {"Assistant", "Fixer", "Tester"};
        for (int i = 0; i < agentNames.length; i++) {
            double theta = (2 * Math.PI * i) / agentNames.length;
            double x = Math.cos(theta) * 200;
            double z = Math.sin(theta) * 200;
            Map<String, Object> agent = new HashMap<>();
            agent.put("id", "agent_" + i);
            agent.put("name", agentNames[i]);
            agent.put("position", new double[]{x, 100, z});
            agent.put("status", "active");
            agent.put("color", 0x00ffaa);
            agentNodes.add(agent);
        }
        return agentNodes;
    }

    private List<Map<String, Object>> generateDecisionIndicators() {
        List<Map<String, Object>> indicators = new ArrayList<>();
        Map<String, Object> consensusIndicator = new HashMap<>();
        consensusIndicator.put("id", "consensus_vote");
        consensusIndicator.put("position", new double[]{0, 150, 0});
        consensusIndicator.put("color", 0x00ff00);
        consensusIndicator.put("progress", 66.7f);
        indicators.add(consensusIndicator);
        return indicators;
    }

    private Map<String, Object> createNode(String id, String label, double x, double y, double z, int color, float scale) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", id);
        node.put("label", label);
        node.put("position", new double[]{x, y, z});
        node.put("color", color);
        node.put("scale", scale);
        return node;
    }

    public Map<String, Object> getCurrentFrame() {
        return generateVisualizationFrame();
    }

    public Map<String, Object> getVisualizationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("connectedClients", visualizationSessions.size());
        stats.put("frameRate", 30);
        stats.put("costGalaxyActive", true);
        return stats;
    }
}
