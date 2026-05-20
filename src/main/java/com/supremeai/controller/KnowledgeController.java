package com.supremeai.controller;

import com.supremeai.service.KnowledgeSeederServiceEnhanced;
import com.supremeai.service.KnowledgeVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KnowledgeController - API for AI agents to contribute to SupremeAI's collective intelligence.
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

    @Autowired
    private KnowledgeSeederServiceEnhanced knowledgeService;
    
    @Autowired
    private KnowledgeVerificationService verificationService;

    /**
     * Seed Knowledge: Endpoint for agents to push new patterns or fixes discovered during a session.
     * 
     * @param request The learning entry following the SystemLearning model schema.
     * @return Response containing the integrated knowledge ID.
     */
    @PostMapping("/seed")
    public Mono<ResponseEntity<Map<String, Object>>> seedKnowledge(@RequestBody Map<String, Object> request) {
        log.debug("Received knowledge seed request for category: {}", request.get("category"));
        
        return knowledgeService.seed(request)
                .map(id -> ResponseEntity.ok(Map.<String, Object>of(
                        "id", id,
                        "status", "INTEGRATED",
                        "message", "Knowledge successfully added to the system brain."
                )))
                .onErrorResume(e -> {
                    log.error("Knowledge seeding failed: {}", e.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(Map.<String, Object>of(
                            "status", "ERROR",
                            "message", "Failed to integrate knowledge: " + e.getMessage()
                    )));
                });
    }

    /**
     * Initialize Foundation Knowledge: One-click seed for all core system patterns.
     * This fulfills the requirement to "seed all knowledge" via API.
     */
    @PostMapping("/init")
    public Mono<ResponseEntity<Map<String, Object>>> initializeFoundation() {
        log.info("Triggering System-Wide Foundation Knowledge Seed...");
        
        List<Map<String, Object>> foundation = new ArrayList<>();

        // 1. Semantic Transition Knowledge
        foundation.add(Map.of(
            "id", "pattern_semantic_analysis",
            "type", "PATTERN",
            "category", "AI_ARCHITECTURE",
            "content", "Heuristic analysis (keyword counting) is replaced by AI semantic evaluation to detect intent and brilliance.",
            "severity", "HIGH",
            "confidence", 0.98
        ));

        // 2. Resilience: Cascading Failure
        foundation.add(Map.of(
            "id", "rl-0001",
            "type", "RESILIENCE",
            "category", "ERROR_HANDLING",
            "content", "Cascading failure detected when >= 3 providers fail. Mitigation: Open circuit breakers and throttle remaining to 50%.",
            "severity", "CRITICAL",
            "confidence", 0.97
        ));

        // 3. Copilot Workflow: Scaffolding
        foundation.add(Map.of(
            "id", "pattern_copilot_init",
            "type", "PATTERN",
            "category", "PROJECT_CREATION",
            "content", "Copilot project-initialization sequence: scaffold with stack name, accept structure, and generate CI/CD in one prompt.",
            "severity", "HIGH",
            "confidence", 0.96
        ));

        // 4. Data Lifecycle: Simulator Safety
        foundation.add(Map.of(
            "id", "pattern_simulator_ttl",
            "type", "PATTERN",
            "category", "INFRASTRUCTURE",
            "content", "Cloud simulator sessions must register with DataLifecycleService immediately to prevent 'zombie' instance resource leaks.",
            "severity", "CRITICAL",
            "confidence", 0.97
        ));

        return knowledgeService.seedBulk(foundation)
                .map(count -> ResponseEntity.ok(Map.<String, Object>of(
                        "status", "SUCCESS",
                        "seededCount", count,
                        "message", "Foundation knowledge successfully injected into the system brain."
                )))
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body(Map.<String, Object>of(
                        "status", "FAILED",
                        "error", e.getMessage()
                ))));
    }

    /**
     * Verify Foundation Knowledge: Checks if all critical foundation knowledge entries exist
     * and meet a minimum confidence score.
     * This is crucial for system integrity and self-healing capabilities.
     *
     * @param minConfidence The minimum confidence score required for each entry (default 0.90).
     * @return A Mono containing a map with verification results.
     */
    @GetMapping("/verify-foundation")
    public Mono<ResponseEntity<Map<String, Object>>> verifyFoundation(
            @RequestParam(defaultValue = "0.90") double minConfidence) {
        log.info("Verifying foundation knowledge with minimum confidence: {}", minConfidence);

        List<String> foundationIds = verificationService.getFoundationKnowledgeIds();

        return verificationService.verifyFoundationKnowledge(foundationIds, minConfidence)
                .map(ResponseEntity::ok);
    }
}
