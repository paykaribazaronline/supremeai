package org.example.controller;

import org.example.model.Agent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Agents Controller
 * Manages AI agents and their configurations
 */
@RestController
@RequestMapping("/api/ai/agents")
@CrossOrigin(origins = "*")
public class AIAgentsController {

    private static final List<Agent> agents = new ArrayList<>();

    static {
        // Initialize with sample agents
        agents.add(new Agent("agent-1", "Agent-1", Agent.Role.BUILDER, "gpt-4"));
        agents.add(new Agent("agent-2", "Agent-2", Agent.Role.REVIEWER, "gpt-3.5-turbo"));
        agents.add(new Agent("agent-3", "Agent-3", Agent.Role.ARCHITECT, "claude-3"));
    }

    @GetMapping
    public ResponseEntity<?> getAgents() {
        try {
            return ResponseEntity.ok(agents);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAgent(@PathVariable String id) {
        try {
            Optional<Agent> agent = agents.stream()
                .filter(a -> a.getName().equals(id))
                .findFirst();
            if (agent.isPresent()) {
                return ResponseEntity.ok(agent.get());
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Agent not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
