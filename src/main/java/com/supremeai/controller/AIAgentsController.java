package com.supremeai.controller;

import com.supremeai.model.Agent;
import com.supremeai.repository.AgentRepository;
import com.supremeai.util.IdUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-agents")
public class AIAgentsController {

    private final AgentRepository agentRepository;

    public AIAgentsController(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @GetMapping
    public Flux<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    @GetMapping("/stats")
    public Mono<Map<String, Object>> getAgentStats() {
        return agentRepository.findAll().collectList().map(agents -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAgents", agents.size());
            stats.put("activeAgents", agents.stream().filter(a -> "ACTIVE".equals(a.getStatus())).count());
            stats.put("idleAgents", agents.stream().filter(a -> "IDLE".equals(a.getStatus())).count());
            return stats;
        });
    }

    @PostMapping
    public Mono<Agent> createAgent(@RequestBody Agent agent) {
        agent.setId(IdUtils.ensureId(agent.getId()));
        if (agent.getStatus() == null) {
            agent.setStatus("IDLE");
        }
        return agentRepository.save(agent);
    }

    @PutMapping("/{id}/status")
    public Mono<Agent> updateAgentStatus(@PathVariable String id, @RequestParam String status) {
        return agentRepository.findById(id)
                .flatMap(agent -> {
                    agent.setStatus(status);
                    return agentRepository.save(agent);
                });
    }

    @DeleteMapping("/{id}")
    public Mono<Void> removeAgent(@PathVariable String id) {
        return agentRepository.deleteById(id);
    }
}
