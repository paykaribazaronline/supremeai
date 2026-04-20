package com.supremeai.agentorchestration;

import com.supremeai.service.CodeGenerationService;
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

    @Autowired
    private CodeGenerationService codeGenerationService;

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

    @PostMapping("/generate")
    public Mono<ResponseEntity<Object>> orchestrateAndGenerate(@RequestBody Map<String, Object> request) {
        String requirement = (String) request.get("requirement");
        
        if (requirement == null || requirement.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "Requirement is required")));
        }

        try {
            // Step 1: Orchestrate to get decisions
            OrchesResultContext orchestrationResult = orchestrator.orchestrate(requirement);
            Map<String, Object> generationContext = orchestrationResult.getGenerationContext();
            
            // Step 2: Generate code using the context (cast to Map<String, String>)
            @SuppressWarnings("unchecked")
            Map<String, String> decisions = (Map<String, String>) (Map<?, ?>) generationContext;
            Map<String, Object> codeResult = codeGenerationService.generateFromContext(decisions);
            
            // Step 3: Combine response
            Map<String, Object> combined = new java.util.LinkedHashMap<>();
            combined.put("status", "COMPLETED");
            combined.put("requirement", requirement);
            combined.put("decisions", orchestrationResult.getContext().get("decisions"));
            combined.put("generationContext", generationContext);
            combined.put("generatedApp", codeResult);
            
            return Mono.just(ResponseEntity.ok((Object) combined));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(500)
                .body(Map.of("error", "Orchestration+Generation failed: " + e.getMessage())));
        }
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "status", "UP",
            "service", "AdaptiveAgentOrchestrator",
            "components", List.of("RequirementAnalyzer", "ConsensusVoting", "ContextBuilder", "CodeGeneration")
        )));
    }

    @PostMapping("/generate-with-context")
    @SuppressWarnings("unchecked")
    public Mono<ResponseEntity<Object>> generateWithContext(@RequestBody Map<String, Object> request) {
        Map<String, Object> context = (Map<String, Object>) request.get("context");
        if (context == null) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "Context is required")));
        }
        @SuppressWarnings("unchecked")
        Map<String, String> decisions = (Map<String, String>) (Map<?, ?>) context;
        Map<String, Object> result = codeGenerationService.generateFromContext(decisions);
        return Mono.just(ResponseEntity.ok((Object) result));
    }
}
