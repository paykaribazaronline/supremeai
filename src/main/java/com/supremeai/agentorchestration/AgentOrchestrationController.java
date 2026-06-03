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
    public AgentOrchestrationController(AdaptiveAgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public AgentOrchestrationController(com.supremeai.service.AppOrchestrationService orchestrationService, CodeGenerationService codeGenerationService, com.supremeai.agent.GPublishAgent publishAgent) {
        this.orchestrationService = orchestrationService;
        this.codeGenerationService = codeGenerationService;
        this.publishAgent = publishAgent;
    }






    @PostMapping("/requirement")
    public Mono<ResponseEntity<Object>> orchestrate(@RequestBody Map<String, Object> request) {
        String requirement = (String) request.get("requirement");
        
        if (requirement == null || requirement.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "Requirement is required")));
        }

        if (orchestrator == null) {
            return Mono.just(ResponseEntity.status(503)
                .body(Map.of("error", "Orchestrator unavailable in local mode. Set up Firestore credentials.")));
        }

        try {
            OrchesResultContext result = orchestrator.orchestrate(requirement);
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("status", result.getStatus());
            response.put("requirement", requirement);
            response.put("context", result.getContext());
            response.put("generationContext", result.getGenerationContext());
            response.put("completedAt", result.getCompletedAt());
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

        @SuppressWarnings("unchecked")
        Map<String, String> githubConfig = (Map<String, String>) request.get("githubConfig");

        return orchestrationService.runFullPipeline(requirement, githubConfig)
            .map(result -> ResponseEntity.ok((Object) result))
            .onErrorResume(e -> Mono.just(ResponseEntity.status(500)
                .body(Map.of("error", "Full pipeline failed: " + e.getMessage()))));
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
        Map<String, String> decisions = (Map<String, String>) (Map<?, ?>) context;
        Map<String, Object> result = codeGenerationService.generateFromContext(decisions);
        return Mono.just(ResponseEntity.ok((Object) result));
    }

    /**
     * পাবলিশিং প্ল্যান তৈরি এন্ডপয়েন্ট
     * প্ল্যাটফর্ম এবং কনফিগারেশন অনুযায়ী পাবলিশিং প্ল্যান তৈরি করে
     */
    @PostMapping("/publishing-plan")
    @SuppressWarnings("unchecked")
    public Mono<ResponseEntity<Object>> createPublishingPlan(@RequestBody Map<String, Object> request) {
        String platform = (String) request.get("platform");
        Map<String, String> config = (Map<String, String>) request.get("config");

        if (platform == null || platform.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "Platform is required")));
        }

        if (config == null) {
            config = new java.util.HashMap<>();
        }

        try {
            Map<String, String> plan = publishAgent.createPublishingPlan(platform, config);

            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("status", "SUCCESS");
            response.put("platform", platform);
            response.put("publishingPlan", plan);

            return Mono.just(ResponseEntity.ok((Object) response));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(500)
                .body(Map.of("error", "Publishing plan creation failed: " + e.getMessage())));
        }
    }
}
