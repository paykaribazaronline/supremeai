package com.supremeai.controller;

import com.supremeai.service.EnhancedLearningService;
import com.supremeai.model.SystemLearning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Enhanced Learning Controller - Exposes all learning type endpoints
 * Covers NLP, Multimodal, Ecosystem, App Generation, and Predictive learning
 */
@RestController
@RequestMapping("/api/enhanced-learning")
@PreAuthorize("hasRole('ADMIN')")
public class EnhancedLearningController {

    @Autowired
    private EnhancedLearningService enhancedLearningService;

    /**
     * Get learning statistics
     */
    @GetMapping("/stats")
    public Mono<Map<String, Object>> getStats() {
        return enhancedLearningService.getLearningStats();
    }

    /**
     * Get best practices based on past learnings
     */
    @GetMapping("/best-practices/{category}")
    public Flux<SystemLearning> getBestPractices(
            @PathVariable String category,
            @RequestParam(defaultValue = "0.7") double minConfidence) {
        return enhancedLearningService.getBestPractices(category, minConfidence);
    }

    /**
     * Get predictive recommendations for a task
     */
    @GetMapping("/predictive/{taskType}")
    public Flux<SystemLearning> getPredictiveRecommendations(
            @PathVariable String taskType) {
        return enhancedLearningService.getPredictiveRecommendations(taskType, Map.of());
    }

    /**
     * Manually record NLP learning
     */
    @PostMapping("/nlp")
    public Mono<SystemLearning> recordNLPLearning(@RequestBody Map<String, Object> payload) {
        String userInput = (String) payload.get("userInput");
        String aiResponse = (String) payload.get("aiResponse");
        String provider = (String) payload.getOrDefault("provider", "unknown");
        double qualityScore = payload.containsKey("qualityScore") ?
                ((Number) payload.get("qualityScore")).doubleValue() : 0.5;
        Map<String, Object> context = (Map<String, Object>) payload.getOrDefault("context", Map.of());

        return enhancedLearningService.learnFromNLPInteraction(userInput, aiResponse, provider, qualityScore, context);
    }

    /**
     * Manually record Multimodal learning
     */
    @PostMapping("/multimodal")
    public Mono<SystemLearning> recordMultimodalLearning(@RequestBody Map<String, Object> payload) {
        String textPrompt = (String) payload.get("textPrompt");
        String imageUrl = (String) payload.get("imageUrl");
        String generatedCode = (String) payload.get("generatedCode");
        String provider = (String) payload.getOrDefault("provider", "unknown");
        double accuracyScore = payload.containsKey("accuracyScore") ?
                ((Number) payload.get("accuracyScore")).doubleValue() : 0.5;

        return enhancedLearningService.learnFromMultimodalInteraction(textPrompt, imageUrl, generatedCode, provider, accuracyScore);
    }

    /**
     * Manually record API usage learning
     */
    @PostMapping("/ecosystem/api-usage")
    public Mono<SystemLearning> recordAPIUsageLearning(@RequestBody Map<String, Object> payload) {
        String apiEndpoint = (String) payload.get("apiEndpoint");
        String provider = (String) payload.getOrDefault("provider", "unknown");
        long responseTimeMs = payload.containsKey("responseTimeMs") ?
                ((Number) payload.get("responseTimeMs")).longValue() : 0;
        boolean success = payload.containsKey("success") ?
                (Boolean) payload.get("success") : false;
        Map<String, Object> requestMeta = (Map<String, Object>) payload.getOrDefault("requestMeta", Map.of());

        return enhancedLearningService.learnFromAPIUsage(apiEndpoint, provider, responseTimeMs, success, requestMeta);
    }

    /**
     * Manually record App Generation learning
     */
    @PostMapping("/app-generation")
    public Mono<SystemLearning> recordAppGenerationLearning(@RequestBody Map<String, Object> payload) {
        String requirement = (String) payload.get("requirement");
        String generatedAppType = (String) payload.getOrDefault("appType", "android");
        boolean buildSuccess = payload.containsKey("buildSuccess") ?
                (Boolean) payload.get("buildSuccess") : false;
        String apkPath = (String) payload.getOrDefault("apkPath", "");
        Map<String, Object> buildMetrics = (Map<String, Object>) payload.getOrDefault("buildMetrics", Map.of());
        String agentUsed = (String) payload.getOrDefault("agentUsed", "unknown");

        return enhancedLearningService.learnFromAppGeneration(requirement, generatedAppType, buildSuccess, apkPath, buildMetrics, agentUsed);
    }

    /**
     * Manually record Predictive learning
     */
    @PostMapping("/predictive")
    public Mono<SystemLearning> recordPredictiveLearning(@RequestBody Map<String, Object> payload) {
        String patternType = (String) payload.get("patternType");
        Map<String, Object> patternData = (Map<String, Object>) payload.getOrDefault("patternData", Map.of());
        double confidence = payload.containsKey("confidence") ?
                ((Number) payload.get("confidence")).doubleValue() : 0.5;
        String basedOnLearningId = (String) payload.getOrDefault("basedOnLearningId", "");

        return enhancedLearningService.learnPredictivePattern(patternType, patternData, confidence, basedOnLearningId);
    }

    /**
     * Apply a learning (increment counter)
     */
    @PostMapping("/apply/{learningId}")
    public Mono<SystemLearning> applyLearning(@PathVariable String learningId) {
        return enhancedLearningService.applyLearning(learningId);
    }

    /**
     * Get learnings by type
     */
    @GetMapping("/type/{learningType}")
    public Flux<SystemLearning> getByLearningType(@PathVariable String learningType) {
        return enhancedLearningService.getPredictiveRecommendations(learningType, Map.of());
    }
}
