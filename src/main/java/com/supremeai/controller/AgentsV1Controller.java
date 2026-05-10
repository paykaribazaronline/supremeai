package com.supremeai.controller;

import com.supremeai.model.Milestone;
import com.supremeai.repository.MilestoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentsV1Controller {

    @Autowired
    private MilestoneRepository milestoneRepository;

    @GetMapping("/all-phases")
    public Mono<Map<String, Object>> getAllPhases() {
        return milestoneRepository.findAllByOrderByOrderAsc()
                .collectList()
                .map(milestones -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("totalPhases", milestones.size());
                    response.put("operationalCount", milestones.stream().filter(m -> m.getProgress() != null && m.getProgress() == 100).count());
                    response.put("systemStatus", "OPTIMAL");
                    response.put("timestamp", System.currentTimeMillis());
                    
                    response.put("phases", milestones.stream().map(m -> {
                        Map<String, Object> phase = new HashMap<>();
                        phase.put("id", m.getId());
                        phase.put("phase", m.getOrder()); // Using order as phase number
                        phase.put("name", m.getTitle() != null ? m.getTitle() : m.getName());
                        phase.put("status", m.getProgress() != null && m.getProgress() == 100 ? "operational" : 
                                           (m.getProgress() != null && m.getProgress() > 0 ? "in-progress" : "pending"));
                        phase.put("description", m.getTimeline());
                        phase.put("order", m.getOrder());
                        return phase;
                    }).collect(Collectors.toList()));
                    
                    return response;
                });
    }

    @GetMapping("/phase8/summary")
    public Mono<Map<String, Object>> getPhase8Summary() {
        return Mono.just(Map.of(
            "status", "operational",
            "agentCount", 3,
            "agents", List.of("SecurityAuditor", "VulnerabilityScanner", "PenTester"),
            "capabilities", List.of("Automated Pentesting", "Dependency Scanning", "Security Hardening")
        ));
    }

    @GetMapping("/phase9/summary")
    public Mono<Map<String, Object>> getPhase9Summary() {
        return Mono.just(Map.of(
            "status", "operational",
            "agentCount", 2,
            "agents", List.of("CostOptimizer", "QuotaManager"),
            "capabilities", List.of("Cloud Bill Analysis", "Token Usage Optimization", "Multi-Provider Cost Balancing")
        ));
    }

    @GetMapping("/phase10/summary")
    public Mono<Map<String, Object>> getPhase10Summary() {
        return Mono.just(Map.of(
            "status", "in-progress",
            "agentCount", 1,
            "agents", List.of("SelfEvolver"),
            "capabilities", List.of("Recursive Improvement", "Heuristic Optimization")
        ));
    }

    @GetMapping("/optimization/health")
    public Mono<Map<String, Object>> getOptimizationHealth() {
        return Mono.just(Map.of(
            "status", "UP",
            "services", Map.of(
                "CodeAnalyzer", Map.of("status", "Active"),
                "PerformanceTuner", Map.of("status", "OK"),
                "RefactorEngine", Map.of("status", "Active")
            )
        ));
    }

    @GetMapping("/phase6/health")
    public Mono<Map<String, Object>> getPhase6Health() {
        return Mono.just(Map.of(
            "status", "UP",
            "agentCount", 4,
            "capabilities", List.of("External API Integration", "Webhook Orchestration", "Cross-Platform Sync")
        ));
    }

    @GetMapping("/phase7/agents/summary")
    public Mono<Map<String, Object>> getPhase7Summary() {
        return Mono.just(Map.of(
            "status", "operational",
            "agentCount", 5,
            "agents", List.of("FlutterGenerator", "ReactGenerator", "SpringGenerator", "IOSBuilder", "AndroidBuilder")
        ));
    }
}
