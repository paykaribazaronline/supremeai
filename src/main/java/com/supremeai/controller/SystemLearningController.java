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

    public SystemLearningController(SystemLearningService service,
                                     EnhancedLearningService enhancedService) {
        this.service = service;
        this.enhancedService = enhancedService;
    }

    @GetMapping
    public List<SystemLearning> getAllLearning() {
        return service.getAllLearningSync();
    }

    @GetMapping("/category/{category}")
    public List<SystemLearning> getByCategory(@PathVariable String category) {
        return service.getByCategorySync(category);
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
     * Manually trigger a learning cycle (for testing or admin-initiated learning).
     * Intended for debugging or forcing an immediate improvement pass.
     */
    @PostMapping("/trigger-improvement")
    public Mono<String> triggerImprovement() {
        // Could integrate with SelfImprovementService if needed.
        // For now, acknowledge the endpoint.
        return Mono.just("Improvement trigger not implemented in this context");
    }
}
