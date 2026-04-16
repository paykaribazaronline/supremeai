package com.supremeai.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/ai-agents")
public class AIAgentsController {

    private final List<Map<String, Object>> agents = new ArrayList<>();

    public AIAgentsController() {
        // Mock data for initial implementation
        agents.add(createAgent(1L, "Sentinel-1", "Security Monitor", "ACTIVE"));
        agents.add(createAgent(2L, "Infiltrator-Alpha", "Exploitation Specialist", "IDLE"));
        agents.add(createAgent(3L, "Guardian-X", "Defense Coordinator", "ACTIVE"));
    }

    @GetMapping
    public List<Map<String, Object>> getAllAgents() {
        return agents;
    }

    @GetMapping("/stats")
    public Map<String, Object> getAgentStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAgents", agents.size());
        stats.put("activeAgents", agents.stream().filter(a -> "ACTIVE".equals(a.get("status"))).count());
        stats.put("idleAgents", agents.stream().filter(a -> "IDLE".equals(a.get("status"))).count());
        return stats;
    }

    @PostMapping
    public Map<String, Object> createAgent(@RequestBody Map<String, Object> agent) {
        agent.put("id", (long) (agents.size() + 1));
        agent.put("status", "IDLE");
        agents.add(agent);
        return agent;
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> updateAgentStatus(@PathVariable Long id, @RequestParam String status) {
        for (Map<String, Object> agent : agents) {
            if (agent.get("id").equals(id)) {
                agent.put("status", status);
                return agent;
            }
        }
        throw new RuntimeException("Agent not found");
    }

    @DeleteMapping("/{id}")
    public void removeAgent(@PathVariable Long id) {
        agents.removeIf(a -> a.get("id").equals(id));
    }

    private Map<String, Object> createAgent(Long id, String name, String type, String status) {
        Map<String, Object> agent = new HashMap<>();
        agent.put("id", id);
        agent.put("name", name);
        agent.put("type", type);
        agent.put("status", status);
        agent.put("uptime", "24h");
        return agent;
    }
}
