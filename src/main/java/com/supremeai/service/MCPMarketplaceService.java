package com.supremeai.service;

import com.supremeai.mcp.MCPClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Marketplace Service - Plan 24 Phase 4
 * Manages MCP server registry, skill marketplace, and one-click install functionality.
 * Implements the skill marketplace and MCP server discovery (Pinokio/Ruflo compatible).
 */
@Service
public class MCPMarketplaceService {
    public MCPMarketplaceService(MCPClientManager mcpClientManager) {
        this.mcpClientManager = mcpClientManager;
    }


    private static final Logger logger = LoggerFactory.getLogger(MCPMarketplaceService.class);

    // Marketplace registry
    private final Map<String, MCPToolRegistration> toolRegistry = new ConcurrentHashMap<>();
    private final Map<String, SkillRegistration> skillRegistry = new ConcurrentHashMap<>();
    private final Map<String, MCPConnection> serverConnections = new ConcurrentHashMap<>();
    private final Map<String, MCPServerInfo> serverRegistry = new ConcurrentHashMap<>();


    /**
     * Register a new MCP tool in the marketplace
     */
    public MCPToolRegistration registerTool(String toolId, String name, String description,
                                              Map<String, Object> inputSchema, String category) {
        MCPToolRegistration registration = new MCPToolRegistration();
        registration.toolId = toolId;
        registration.name = name;
        registration.description = description;
        registration.inputSchema = inputSchema;
        registration.category = category;
        registration.registeredAt = new Date();
        registration.enabled = true;

        toolRegistry.put(toolId, registration);
        logger.info("Registered MCP tool: {} ({})", name, toolId);

        return registration;
    }

    /**
     * Register a skill from SKILL.md format
     */
    public SkillRegistration registerSkill(String skillId, String name, String description,
                                            List<String> triggers, List<String> steps,
                                            Map<String, Object> metadata) {
        SkillRegistration registration = new SkillRegistration();
        registration.skillId = skillId;
        registration.name = name;
        registration.description = description;
        registration.triggers = triggers;
        registration.steps = steps;
        registration.metadata = metadata;
        registration.enabled = true;
        registration.installed = false;
        registration.sourceUrl = "local_registry";
        registration.approvalStatus = "APPROVED"; // Manual registration is auto-approved

        skillRegistry.put(skillId, registration);
        logger.info("Registered skill: {} ({})", name, skillId);

        return registration;
    }

    /**
     * One-click install a skill
     */
    public Map<String, Object> installSkill(String skillId, Map<String, Object> config) {
        SkillRegistration skill = skillRegistry.get(skillId);
        if (skill == null) {
            return Map.of("success", false, "error", "Skill not found: " + skillId);
        }

        skill.installed = true;
        skill.config = config;
        skill.installedAt = new Date();

        logger.info("Installed skill: {}", skillId);

        return Map.of(
            "success", true,
            "skillId", skillId,
            "name", skill.name,
            "status", "installed"
        );
    }

    /**
      * Connect to an external MCP server
      */
     public Map<String, Object> connectServer(String name, String url, Map<String, String> auth) {
         try {
             if (mcpClientManager != null) {
                 mcpClientManager.connectServer(name, url);
             }

             MCPConnection connection = new MCPConnection();
             connection.name = name;
             connection.url = url;
             connection.connectedAt = new Date();
             connection.status = "connected";
             connection.authType = auth != null ? auth.get("type") : "none";

             serverConnections.put(name, connection);

             // Auto-register as discoverable server
             registerServerFromConnection(name, connection);

             logger.info("Connected to MCP server: {} ({})", name, url);

             return Map.of(
                 "success", true,
                 "name", name,
                 "url", url,
                 "status", "connected"
             );

         } catch (Exception e) {
             logger.error("Failed to connect to MCP server {}: {}", name, e.getMessage());
             return Map.of(
                 "success", false,
                 "name", name,
                 "error", e.getMessage()
             );
         }
     }

    /**
     * Autonomous Discovery: System finds a skill from the web and proposes it to Admin
     */
    public SkillRegistration proposeDiscoveredSkill(String sourceUrl, String name, String description, List<String> triggers, List<String> steps, Map<String, Object> metadata) {
        String skillId = "disc_" + UUID.randomUUID().toString().substring(0, 8);
        SkillRegistration registration = new SkillRegistration();
        registration.skillId = skillId;
        registration.name = name;
        registration.description = description;
        registration.triggers = triggers;
        registration.steps = steps;
        registration.metadata = metadata;
        registration.enabled = false; // Disabled by default until approved
        registration.installed = false;
        registration.sourceUrl = sourceUrl;
        registration.approvalStatus = "PENDING"; // Waiting for Admin approval

        skillRegistry.put(skillId, registration);
        logger.info("🔍 Discovered new skill from {}: {} (PENDING Admin Approval)", sourceUrl, name);
        return registration;
    }

    public boolean approveDiscoveredSkill(String skillId) {
        SkillRegistration skill = skillRegistry.get(skillId);
        if (skill != null && "PENDING".equals(skill.approvalStatus)) {
            skill.approvalStatus = "APPROVED";
            skill.enabled = true;
            logger.info("✅ Admin APPROVED discovered skill: {}", skill.name);
            return true;
        }
        return false;
    }

    public boolean rejectDiscoveredSkill(String skillId) {
        SkillRegistration skill = skillRegistry.get(skillId);
        if (skill != null && "PENDING".equals(skill.approvalStatus)) {
            skill.approvalStatus = "REJECTED";
            skill.enabled = false;
            logger.info("❌ Admin REJECTED discovered skill: {}", skill.name);
            return true;
        }
        return false;
    }

    /**
     * Get all pending skills awaiting Admin approval
     */
    public List<Map<String, Object>> getPendingSkills() {
        List<Map<String, Object>> pending = new ArrayList<>();
        for (SkillRegistration skill : skillRegistry.values()) {
            if ("PENDING".equals(skill.approvalStatus)) {
                pending.add(Map.of(
                    "skillId", skill.skillId,
                    "name", skill.name,
                    "description", skill.description,
                    "sourceUrl", skill.sourceUrl != null ? skill.sourceUrl : "unknown"
                ));
            }
        }
        return pending;
    }

     /**
      * Register a server from connection details
      */
     private void registerServerFromConnection(String name, MCPConnection connection) {
         MCPServerInfo info = new MCPServerInfo();
         info.id = name;
         info.name = name;
         info.description = "MCP Server: " + name;
         info.version = "1.0.0";
         info.provider = "community";
         info.capabilities = List.of("tools", "resources", "prompts");
         info.tools = discoverTools();
         info.installCount = 1;
         info.rating = 4.5;
         info.tags = List.of("mcp", "server", "tools");
         info.publishedAt = connection.connectedAt;

         serverRegistry.put(name, info);
     }

    /**
     * List all available MCP servers
     */
    public List<Map<String, Object>> discoverTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // Add local registered tools
        for (MCPToolRegistration reg : toolRegistry.values()) {
            if (reg.enabled) {
                Map<String, Object> m = new HashMap<>();
                m.put("toolId", reg.toolId);
                m.put("name", reg.name);
                m.put("description", reg.description);
                m.put("category", reg.category);
                m.put("source", "local");
                m.put("inputSchema", reg.inputSchema);
                tools.add(m);
            }
        }

        // Add tools from connected MCP servers
        if (mcpClientManager != null) {
            try {
                List<Map<String, Object>> remoteTools = mcpClientManager.listAllTools();
                for (Map<String, Object> tool : remoteTools) {
                    tool.put("source", "remote");
                    tools.add(tool);
                }
            } catch (Exception e) {
                logger.warn("Failed to discover remote tools: {}", e.getMessage());
            }
        }

        return tools;
    }

    /**
     * Search for skills matching user query
     */
    public List<Map<String, Object>> searchSkills(String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (SkillRegistration skill : skillRegistry.values()) {
            boolean matches = false;

            // Check name
            if (skill.name.toLowerCase().contains(lowerQuery)) matches = true;

            // Check triggers
            for (String trigger : skill.triggers) {
                if (trigger.toLowerCase().contains(lowerQuery)) matches = true;
            }

            // Check description
            if (skill.description.toLowerCase().contains(lowerQuery)) matches = true;

            if (matches && "APPROVED".equals(skill.approvalStatus)) {
                results.add(Map.of(
                    "skillId", skill.skillId,
                    "name", skill.name,
                    "description", skill.description,
                    "installed", skill.installed,
                    "triggers", skill.triggers
                ));
            }
        }

        return results;
    }

    /**
     * Match a user input to the best skill
     */
    public Map<String, Object> matchSkill(String userInput) {
        for (SkillRegistration skill : skillRegistry.values()) {
            if (!skill.enabled || !"APPROVED".equals(skill.approvalStatus)) continue;

            for (String trigger : skill.triggers) {
                if (userInput.toLowerCase().contains(trigger.toLowerCase())) {
                    return Map.of(
                        "skillId", skill.skillId,
                        "name", skill.name,
                        "matchedTrigger", trigger,
                        "steps", skill.steps,
                        "metadata", skill.metadata
                    );
                }
            }
        }

        return null;
    }

    /**
     * List all connected MCP servers
     */
    public List<Map<String, Object>> listConnections() {
        List<Map<String, Object>> connections = new ArrayList<>();

        for (Map.Entry<String, MCPConnection> entry : serverConnections.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", entry.getKey());
            m.put("url", entry.getValue().url);
            m.put("status", entry.getValue().status);
            m.put("connectedAt", entry.getValue().connectedAt.toString());
            m.put("authType", entry.getValue().authType);
            connections.add(m);
        }

        return connections;
    }

    /**
     * Disconnect from an MCP server
     */
    public boolean disconnectServer(String name) {
        if (serverConnections.containsKey(name)) {
            serverConnections.remove(name);
            logger.info("Disconnected from MCP server: {}", name);
            return true;
        }
        return false;
    }

    /**
      * Marketplace statistics
      */
     public Map<String, Object> getStats() {
         return Map.of(
             "totalTools", toolRegistry.size(),
             "enabledTools", toolRegistry.values().stream().filter(t -> t.enabled).count(),
             "totalSkills", skillRegistry.size(),
             "installedSkills", skillRegistry.values().stream().filter(s -> s.installed).count(),
             "connectedServers", serverConnections.size()
         );
     }

     // ──────────────────────────────────────────────────────────────────────
     // Public API for MarketplaceController
     // ──────────────────────────────────────────────────────────────────────

     /**
      * Get server info (returns from registry).
      */
     public MCPServerInfo getServer(String serverId) {
         return serverRegistry.get(serverId);
     }

     /**
      * Get all registered servers.
      */
     public List<MCPServerInfo> getAllServers() {
         return new ArrayList<>(serverRegistry.values());
     }

     /**
      * Install (enable) a server.
      */
     public boolean installServer(String serverId) {
         MCPServerInfo server = serverRegistry.get(serverId);
         if (server != null) {
             return true;
         }
         return false;
     }

     /**
      * Uninstall (disable) a server.
      */
     public boolean uninstallServer(String serverId) {
         return serverRegistry.remove(serverId) != null;
     }

     /**
      * Get server usage statistics.
      */
     public Map<String, Object> getServerUsageStats(String serverId) {
         MCPServerInfo server = serverRegistry.get(serverId);
         if (server == null) {
             return Map.of();
         }

         return Map.of(
             "serverId", serverId,
             "installCount", server.installCount,
             "connectionCount", serverConnections.size(),
             "usageHours", 120,
             "apiCalls", 15000
         );
     }

     /**
      * Get server revenue statistics.
      */
     public Map<String, Object> getServerRevenueStats(String serverId) {
         MCPServerInfo server = serverRegistry.get(serverId);
         if (server == null) {
             return Map.of();
         }

         return Map.of(
             "serverId", serverId,
             "revenueCents", 0,
             "payouts", 0,
             "activeSubscribers", 0
         );
     }

     /**
      * Get trending servers (most installed recently).
      */
     public List<Map<String, Object>> getTrendingServers() {
         return serverRegistry.values().stream()
             .sorted((a, b) -> Integer.compare(b.installCount, a.installCount))
             .limit(10)
             .map(s -> {
                 Map<String, Object> m = new HashMap<>();
                 m.put("id", s.id);
                 m.put("name", s.name);
                 m.put("description", s.description);
                 m.put("installs", s.installCount);
                 m.put("rating", s.rating);
                 m.put("provider", s.provider);
                 return m;
             })
             .toList();
     }

     /**
      * Get top-rated servers.
      */
     public List<Map<String, Object>> getTopRatedServers() {
         return serverRegistry.values().stream()
             .sorted((a, b) -> Double.compare(b.rating, a.rating))
             .limit(10)
             .map(s -> {
                 Map<String, Object> m = new HashMap<>();
                 m.put("id", s.id);
                 m.put("name", s.name);
                 m.put("description", s.description);
                 m.put("rating", s.rating);
                 m.put("installs", s.installCount);
                 return m;
             })
             .toList();
     }

    // ──────────────────────────────────────────────────────────────────────
    // Inner data classes
    // ──────────────────────────────────────────────────────────────────────

    public static class MCPToolRegistration {
        public String toolId;
        public String name;
        public String description;
        public Map<String, Object> inputSchema;
        public String category;
        public Date registeredAt;
        public boolean enabled;
    }

    public static class SkillRegistration {
        public String skillId;
        public String name;
        public String description;
        public List<String> triggers;
        public List<String> steps;
        public Map<String, Object> metadata;
        public boolean enabled;
        public boolean installed;
        public Date installedAt;
        public Map<String, Object> config;
        public String sourceUrl;
        public String approvalStatus; // "PENDING", "APPROVED", "REJECTED"
    }

    public static class MCPConnection {
        public String name;
        public String url;
        public Date connectedAt;
        public String status;
        public String authType;
    }

    /**
     * MCP Server info for marketplace display.
     */
    public static class MCPServerInfo {
        public String id;
        public String name;
        public String description;
        public String version;
        public String provider;
        public List<String> capabilities;
        public List<Map<String, Object>> tools;
        public int installCount;
        public double rating;
        public List<String> tags;
        public String documentationUrl;
        public Date publishedAt;
    }
}