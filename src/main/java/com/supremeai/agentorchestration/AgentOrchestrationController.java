package com.supremeai.agentorchestration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orchestrate")
public class AgentOrchestrationController {

    @Autowired
    private AdaptiveAgentOrchestrator orchestrator;

    @PostMapping("/requirement")
    public Mono<ResponseEntity<Object>> orchestrate(@RequestBody Map<String, Object> request) {
        String requirement = (String) request.get("requirement");
        
        if (requirement == null || requirement.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "Requirement is required")));
        }

        try {
            OrchesResultContext result = orchestrator.orchestrate(requirement);
            Map<String, Object> response = Map.of(
                "status", result.getStatus(),
                "requirement", requirement,
                "context", result.getContext(),
                "generationContext", result.getGenerationContext(),
                "completedAt", result.getCompletedAt()
            );
            return Mono.just(ResponseEntity.ok((Object) response));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(500)
                .body(Map.of("error", "Orchestration failed: " + e.getMessage())));
        }
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "status", "UP",
            "service", "AdaptiveAgentOrchestrator",
            "components", List.of("RequirementAnalyzer", "ConsensusVoting", "ContextBuilder")
        )));
    }

    @PostMapping("/generate-with-context")
    public Mono<ResponseEntity<Object>> generateWithContext(@RequestBody Map<String, Object> request) {
        Map<String, Object> context = (Map<String, Object>) request.get("context");
        if (context == null) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "Context is required")));
        }
        // Defer to CodeGenerationService via injection
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "status", "NOT_IMPLEMENTED",
            "message", "Direct context generation not yet connected"
        )));
    }
}
