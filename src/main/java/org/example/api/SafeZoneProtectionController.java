package org.example.api;

import org.example.service.MemoryManager;
import org.example.service.SafeZoneManager;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Phase 2: SafeZone Protection Controller
 * 
 * REST API endpoints for managing protected AI agents.
 * Protected agents cannot be auto-demoted or rotated by the system.
 * Only admins can protect/unprotect agents.
 * 
 * Endpoints:
 * - GET /api/safezone/protected - List all protected agents
 * - POST /api/safezone/protect/{agentId} - Mark agent as protected
 * - DELETE /api/safezone/unprotect/{agentId} - Remove protection
 * - GET /api/safezone/stats - Get safezone statistics
 */
@RestController
@RequestMapping("/api/safezone")
@CrossOrigin(origins = "*")
public class SafeZoneProtectionController {
    
    private final MemoryManager memoryManager;
    private final SafeZoneManager safeZoneManager;
    
    // In-memory protection map (agentId -> protectionInfo)
    private final Map<String, Map<String, Object>> protectionMap = new HashMap<>();
    
    public SafeZoneProtectionController(MemoryManager memoryManager, SafeZoneManager safeZoneManager) {
        this.memoryManager = memoryManager;
        this.safeZoneManager = safeZoneManager;
        
        // Initialize with existing safezone agents
        for (String agentId : memoryManager.getSafezoneAgents()) {
            Map<String, Object> info = new HashMap<>();
            info.put("agentId", agentId);
            info.put("protected", true);
            info.put("protectedAt", new Date());
            info.put("reason", "Previously protected");
            protectionMap.put(agentId, info);
        }
    }
    
    /**
     * GET /api/safezone/protected
     * 
     * List all protected agents in the system.
     * Protected agents have special protection status and show:
     * - Agent ID
     * - Protection timestamp
     * - Reason for protection
     * - Performance metrics (success rate, avg time)
     * 
     * @return List of protected agents with details
     */
    @GetMapping("/protected")
    public Map<String, Object> listProtectedAgents() {
        List<Map<String, Object>> protected_agents = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : protectionMap.entrySet()) {
            String agentId = entry.getKey();
            Map<String, Object> info = entry.getValue();
            
            // Add performance metrics
            Map<String, Object> scoreboard = memoryManager.getAIScoreboard();
            if (scoreboard.containsKey(agentId)) {
                Map<String, Object> score = (Map<String, Object>) scoreboard.get(agentId);
                info.put("successCount", score.get("success_count"));
                info.put("failCount", score.get("fail_count"));
                info.put("avgTime", score.get("avg_time"));
            }
            
            protected_agents.add(info);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("total", protected_agents.size());
        response.put("protected_agents", protected_agents);
        response.put("timestamp", new Date());
        
        return response;
    }
    
    /**
     * POST /api/safezone/protect/{agentId}
     * 
     * Mark an agent as protected (admin only).
     * Protected agents:
     * - Cannot be auto-rotated by failureThreshold logic
     * - Cannot be auto-demoted by poor performance
     * - Remain available for manual assignment
     * 
     * Request body (optional):
     * {
     *   "reason": "Critical production agent",
     *   "adminId": "admin-001"
     * }
     * 
     * @param agentId The agent ID to protect
     * @param requestBody Optional protection metadata
     * @return Confirmation of protection
     */
    @PostMapping("/protect/{agentId}")
    public Map<String, Object> protectAgent(
            @PathVariable String agentId,
            @RequestBody(required = false) Map<String, String> requestBody) {
        
        String reason = (requestBody != null && requestBody.containsKey("reason")) 
            ? requestBody.get("reason") 
            : "Protected by admin";
        
        // Add to safezone in memory
        memoryManager.addToSafezone(agentId);
        
        // Track in local map
        Map<String, Object> protectionInfo = new HashMap<>();
        protectionInfo.put("agentId", agentId);
        protectionInfo.put("protected", true);
        protectionInfo.put("protectedAt", new Date());
        protectionInfo.put("reason", reason);
        protectionInfo.put("adminId", requestBody != null ? requestBody.get("adminId") : "system");
        protectionMap.put(agentId, protectionInfo);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Agent " + agentId + " is now protected");
        response.put("agentId", agentId);
        response.put("reason", reason);
        response.put("protectedAt", new Date());
        
        return response;
    }
    
    /**
     * DELETE /api/safezone/unprotect/{agentId}
     * 
     * Remove protection from an agent (admin only).
     * Unprotected agents will again be subject to:
     * - Auto-rotation if quota limits are approached
     * - Auto-demotion if performance drops
     * - Fallback chain management
     * 
     * @param agentId The agent ID to unprotect
     * @return Confirmation of removal
     */
    @DeleteMapping("/unprotect/{agentId}")
    public Map<String, Object> unprotectAgent(@PathVariable String agentId) {
        
        // Remove from safezone
        memoryManager.getSafezoneAgents().remove(agentId);
        protectionMap.remove(agentId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Agent " + agentId + " protection removed");
        response.put("agentId", agentId);
        response.put("unprotectedAt", new Date());
        
        return response;
    }
    
    /**
     * GET /api/safezone/stats
     * 
     * Get safezone protection statistics and monitoring.
     * Shows:
     * - Total protected agents
     * - Total agents in system
     * - Protection coverage percentage
     * - Average performance of protected agents
     * - Most recently protected agent
     * 
     * @return SafeZone statistics
     */
    @GetMapping("/stats")
    public Map<String, Object> getSafezoneStats() {
        Map<String, Object> scoreboard = memoryManager.getAIScoreboard();
        List<String> protectedAgents = memoryManager.getSafezoneAgents();
        
        // Calculate protected agent performance
        double protectedAvgScore = protectedAgents.stream()
                .mapToDouble(memoryManager::calculateAgentScore)
                .average()
                .orElse(0.0);
        
        double systemAvgScore = scoreboard.keySet().stream()
                .mapToDouble(memoryManager::calculateAgentScore)
                .average()
                .orElse(0.0);
        
        // Find most recently protected
        String mostRecent = protectedAgents.isEmpty() ? "none" : 
            protectedMap.entrySet().stream()
                .filter(e -> protectedAgents.contains(e.getKey()))
                .min(Comparator.comparing(e -> ((Date) ((Map<String, Object>) e.getValue()).get("protectedAt"))))
                .map(Map.Entry::getKey)
                .orElse("unknown");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("status", "success");
        stats.put("total_protected_agents", protectedAgents.size());
        stats.put("total_agents", scoreboard.size());
        stats.put("protection_coverage_pct", 
            scoreboard.size() > 0 ? ((double) protectedAgents.size() / scoreboard.size()) * 100 : 0);
        stats.put("protected_agents_avg_score", String.format("%.2f", protectedAvgScore));
        stats.put("system_avg_score", String.format("%.2f", systemAvgScore));
        stats.put("performance_improvement", 
            String.format("%.2f%%", (protectedAvgScore - systemAvgScore) * 100 / Math.max(systemAvgScore, 0.01)));
        stats.put("most_recent_protected", mostRecent);
        stats.put("protected_agent_list", protectedAgents);
        stats.put("timestamp", new Date());
        
        return stats;
    }
    
    /**
     * GET /api/safezone/is-protected/{agentId}
     * 
     * Check if a specific agent is protected.
     * 
     * @param agentId The agent ID to check
     * @return Protection status
     */
    @GetMapping("/is-protected/{agentId}")
    public Map<String, Object> isAgentProtected(@PathVariable String agentId) {
        boolean isProtected = memoryManager.isAgentInSafezone(agentId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("agentId", agentId);
        response.put("protected", isProtected);
        
        if (isProtected && protectionMap.containsKey(agentId)) {
            response.put("details", protectionMap.get(agentId));
        }
        
        return response;
    }
    
    /**
     * POST /api/safezone/bulk-protect
     * 
     * Protect multiple agents at once (admin only).
     * 
     * Request body:
     * {
     *   "agentIds": ["agent-1", "agent-2", "agent-3"],
     *   "reason": "Critical production agents"
     * }
     * 
     * @param request Bulk protection request
     * @return Summary of protection results
     */
    @PostMapping("/bulk-protect")
    public Map<String, Object> bulkProtect(@RequestBody Map<String, Object> request) {
        List<String> agentIds = (List<String>) request.get("agentIds");
        String reason = (String) request.getOrDefault("reason", "Bulk protected");
        
        List<String> protected_agents = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        
        for (String agentId : agentIds) {
            try {
                memoryManager.addToSafezone(agentId);
                
                Map<String, Object> info = new HashMap<>();
                info.put("agentId", agentId);
                info.put("protected", true);
                info.put("protectedAt", new Date());
                info.put("reason", reason);
                protectionMap.put(agentId, info);
                
                protected_agents.add(agentId);
            } catch (Exception e) {
                failures.add(agentId + ": " + e.getMessage());
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", failures.isEmpty() ? "success" : "partial");
        response.put("protected_count", protected_agents.size());
        response.put("protected_agents", protected_agents);
        response.put("failures", failures);
        response.put("timestamp", new Date());
        
        return response;
    }
    
    // Helper map - should be synchronized with MemoryManager
    private final Map<String, Map<String, Object>> protectedMap = new HashMap<>();
}
