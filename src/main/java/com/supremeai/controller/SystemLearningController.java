package com.supremeai.controller;

import com.supremeai.model.SystemLearning;
import com.supremeai.service.EnhancedLearningService;
import com.supremeai.service.SystemLearningService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * System learning endpoints - ADMIN only.
 * Stores AI system improvements and learnings.
 */
@RestController
@RequestMapping("/api/system-learning")
@PreAuthorize("hasRole('ADMIN')")
public class SystemLearningController {

    private final SystemLearningService service;
    private final EnhancedLearningService enhancedService;
    private final com.supremeai.service.CyberSecuritySkillService cyberSkillService;

    public SystemLearningController(SystemLearningService service,
                                     EnhancedLearningService enhancedService,
                                     com.supremeai.service.CyberSecuritySkillService cyberSkillService) {
        this.service = service;
        this.enhancedService = enhancedService;
        this.cyberSkillService = cyberSkillService;
    }

    @GetMapping
    public Flux<SystemLearning> getAllLearning() {
        return service.getAllLearning();
    }

    @GetMapping("/category/{category}")
    public Flux<SystemLearning> getByCategory(@PathVariable String category) {
        return service.getByCategory(category);
    }

    @PostMapping
    public Mono<SystemLearning> addLearning(@RequestBody SystemLearning learning) {
        return service.addLearning(learning);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteLearning(@PathVariable String id) {
        return service.deleteLearning(id);
    }

    @GetMapping("/wisdom")
    public Mono<List<SystemLearning>> getSystemWisdom() {
        return service.getAllLearning()
            .filter(l -> l.getConfidenceScore() != null && l.getConfidenceScore() >= 0.85)
            .sort((a, b) -> b.getLearnedAt().compareTo(a.getLearnedAt()))
            .take(20)
            .collectList();
    }

    /**
     * Get comprehensive learning statistics.
     */
    @GetMapping("/stats")
    public Mono<Map<String, Object>> getStats() {
        return enhancedService.getLearningStats();
    }

    /**
     * Get best practices for a given category above a quality threshold.
     */
    @GetMapping("/best-practices/{category}")
    public Flux<SystemLearning> getBestPractices(@PathVariable String category,
                                                 @RequestParam(defaultValue = "0.8") double minQuality) {
        return enhancedService.getBestPractices(category, minQuality);
    }

    /**
     * Get predictive recommendations for a task type.
     */
    @GetMapping("/recommendations")
    public Flux<SystemLearning> getRecommendations(@RequestParam String taskType) {
        return enhancedService.getPredictiveRecommendations(taskType, Map.of());
    }

    /**
     * Manually trigger a learning improvement cycle.
     * Analyzes collected patterns, optimizes knowledge base, and generates improvements.
     * ADMIN only endpoint for system maintenance.
     */
    @PostMapping("/trigger-improvement")
    public Mono<Map<String, Object>> triggerImprovement() {
        return enhancedService.improveSystemLearning();
    }

    /**
     * System learning improvement endpoint for CLI and automation.
     * Collects error data, analyzes patterns, and updates knowledge base.
     * More comprehensive than trigger-improvement with detailed reporting.
     */
    @PostMapping("/improve")
    public Mono<Map<String, Object>> improveLearning() {
        return enhancedService.improveSystemLearning();
    }

    /**
     * Trigger autonomous research on a specific cybersecurity topic.
     * System learns hacking techniques to strengthen its own defense.
     */
    @PostMapping("/cyber-research")
    public Mono<Map<String, Object>> triggerCyberResearch(@RequestParam String topic) {
        return cyberSkillService.initiateLearningCycle(topic);
    }

    /**
     * Get the current status of learned hacking skills and active protections.
     */
    @GetMapping("/cyber-status")
    public Mono<Map<String, Object>> getCyberStatus() {
        return Mono.zip(
            cyberSkillService.getLearnedSkills().collectList(),
            cyberSkillService.getActiveProtections().collectList(),
            cyberSkillService.runSelfAudit()
        ).map(tuple -> Map.of(
            "skills", tuple.getT1(),
            "protections", tuple.getT2(),
            "lastAudit", tuple.getT3()
        ));
    }
}
