package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Predictive Analysis Service - Analyzes past learnings to generate predictive patterns
 * Uses successful generations to improve future outcomes
 */
@Service
public class PredictiveAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(PredictiveAnalysisService.class);

    @Autowired
    private SystemLearningRepository repository;

    @Autowired
    private EnhancedLearningService enhancedLearningService;

    /**
     * Scheduled task: Analyze past learnings every hour and generate predictive patterns
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    public void analyzeAndGeneratePredictivePatterns() {
        logger.info("Starting predictive pattern analysis...");

        repository.findAll()
                .filter(l -> l.getSuccess() != null && l.getSuccess())
                .filter(l -> l.getQualityScore() != null && l.getQualityScore() >= 0.7)
                .collectList()
                .flatMapMany(successfulLearnings -> {
                    logger.info("Found {} successful learnings to analyze", successfulLearnings.size());
                    return analyzePatterns(successfulLearnings);
                })
                .subscribe();
    }

    /**
     * Analyze patterns from successful learnings
     */
    private Flux<SystemLearning> analyzePatterns(List<SystemLearning> successfulLearnings) {
        List<Mono<SystemLearning>> patternMonos = new ArrayList<>();

        // Pattern 1: Analyze successful app generations by platform
        patternMonos.add(analyzePlatformSuccessPatterns(successfulLearnings));

        // Pattern 2: Analyze successful NLP interactions by provider
        patternMonos.add(analyzeNLPProviderPatterns(successfulLearnings));

        // Pattern 3: Analyze API usage patterns for reliability
        patternMonos.add(analyzeAPIReliabilityPatterns(successfulLearnings));

        // Pattern 4: Analyze multimodal success patterns
        patternMonos.add(analyzeMultimodalPatterns(successfulLearnings));

        return Flux.merge(patternMonos);
    }

    /**
     * Pattern: Which platforms have highest success rates?
     */
    private Mono<SystemLearning> analyzePlatformSuccessPatterns(List<SystemLearning> learnings) {
        Map<String, Long> platformSuccessCounts = learnings.stream()
                .filter(l -> "APP_GENERATION".equals(l.getLearningType()))
                .filter(l -> l.getInputData() != null && l.getInputData().containsKey("platform"))
                .collect(Collectors.groupingBy(
                        l -> (String) l.getInputData().get("platform"),
                        Collectors.counting()
                ));

        if (platformSuccessCounts.isEmpty()) {
            return Mono.empty();
        }

        // Find best platform
        String bestPlatform = platformSuccessCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        long totalSuccess = platformSuccessCounts.values().stream().mapToLong(Long::longValue).sum();
        double confidence = (double) platformSuccessCounts.get(bestPlatform) / totalSuccess;

        Map<String, Object> patternData = new HashMap<>();
        patternData.put("bestPlatform", bestPlatform);
        patternData.put("platformCounts", platformSuccessCounts);
        patternData.put("totalSuccess", totalSuccess);

        return enhancedLearningService.learnPredictivePattern(
                "BEST_PLATFORM_" + bestPlatform,
                patternData,
                confidence,
                "platform_analysis"
        );
    }

    /**
     * Pattern: Which NLP providers perform best for different tasks?
     */
    private Mono<SystemLearning> analyzeNLPProviderPatterns(List<SystemLearning> learnings) {
        Map<String, List<SystemLearning>> nlpByProvider = learnings.stream()
                .filter(l -> "NLP".equals(l.getLearningType()))
                .filter(l -> l.getRelatedProvider() != null)
                .collect(Collectors.groupingBy(SystemLearning::getRelatedProvider));

        if (nlpByProvider.isEmpty()) {
            return Mono.empty();
        }

        // Calculate average quality score per provider
        Map<String, Double> providerAvgQuality = new HashMap<>();
        for (Map.Entry<String, List<SystemLearning>> entry : nlpByProvider.entrySet()) {
            double avg = entry.getValue().stream()
                    .filter(l -> l.getQualityScore() != null)
                    .mapToDouble(SystemLearning::getQualityScore)
                    .average()
                    .orElse(0.0);
            providerAvgQuality.put(entry.getKey(), avg);
        }

        // Find best provider
        String bestProvider = providerAvgQuality.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        double confidence = providerAvgQuality.getOrDefault(bestProvider, 0.5);

        Map<String, Object> patternData = new HashMap<>();
        patternData.put("bestProvider", bestProvider);
        patternData.put("providerQualityScores", providerAvgQuality);

        return enhancedLearningService.learnPredictivePattern(
                "BEST_NLP_PROVIDER_" + bestProvider,
                patternData,
                confidence,
                "nlp_provider_analysis"
        );
    }

    /**
     * Pattern: Which API providers are most reliable?
     */
    private Mono<SystemLearning> analyzeAPIReliabilityPatterns(List<SystemLearning> learnings) {
        Map<String, List<SystemLearning>> apiByProvider = learnings.stream()
                .filter(l -> "ECOSYSTEM".equals(l.getLearningType()))
                .filter(l -> l.getRelatedProvider() != null)
                .collect(Collectors.groupingBy(SystemLearning::getRelatedProvider));

        if (apiByProvider.isEmpty()) {
            return Mono.empty();
        }

        // Calculate reliability per provider
        Map<String, Double> providerReliability = new HashMap<>();
        for (Map.Entry<String, List<SystemLearning>> entry : apiByProvider.entrySet()) {
            long total = entry.getValue().size();
            long success = entry.getValue().stream()
                    .filter(l -> l.getSuccess() != null && l.getSuccess())
                    .count();
            double reliability = total > 0 ? (double) success / total : 0.0;
            providerReliability.put(entry.getKey(), reliability);
        }

        // Find most reliable provider
        String mostReliable = providerReliability.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        double confidence = providerReliability.getOrDefault(mostReliable, 0.5);

        Map<String, Object> patternData = new HashMap<>();
        patternData.put("mostReliableProvider", mostReliable);
        patternData.put("providerReliabilityScores", providerReliability);

        return enhancedLearningService.learnPredictivePattern(
                "MOST_RELIABLE_API_" + mostReliable,
                patternData,
                confidence,
                "api_reliability_analysis"
        );
    }

    /**
     * Pattern: Multimodal success patterns
     */
    private Mono<SystemLearning> analyzeMultimodalPatterns(List<SystemLearning> learnings) {
        List<SystemLearning> multimodalLearnings = learnings.stream()
                .filter(l -> "MULTIMODAL".equals(l.getLearningType()))
                .collect(Collectors.toList());

        if (multimodalLearnings.isEmpty()) {
            return Mono.empty();
        }

        // Analyze which providers work best for multimodal
        Map<String, Long> providerCounts = multimodalLearnings.stream()
                .filter(l -> l.getRelatedProvider() != null)
                .collect(Collectors.groupingBy(SystemLearning::getRelatedProvider, Collectors.counting()));

        String bestMultimodalProvider = providerCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        double confidence = multimodalLearnings.size() >= 5 ? 0.8 : 0.5;

        Map<String, Object> patternData = new HashMap<>();
        patternData.put("bestMultimodalProvider", bestMultimodalProvider);
        patternData.put("multimodalCount", multimodalLearnings.size());
        patternData.put("providerCounts", providerCounts);

        return enhancedLearningService.learnPredictivePattern(
                "BEST_MULTIMODAL_PROVIDER_" + bestMultimodalProvider,
                patternData,
                confidence,
                "multimodal_analysis"
        );
    }

    /**
     * Get recommendations for a new task based on past learnings
     */
    public Mono<Map<String, Object>> getRecommendationsForTask(String taskType, Map<String, Object> taskFeatures) {
        return enhancedLearningService.getPredictiveRecommendations(taskType, taskFeatures)
                .collectList()
                .map(patterns -> {
                    Map<String, Object> recommendations = new HashMap<>();
                    recommendations.put("taskType", taskType);
                    recommendations.put("patterns", patterns);
                    recommendations.put("patternCount", patterns.size());

                    // Extract best provider if available
                    if (!patterns.isEmpty()) {
                        SystemLearning bestPattern = patterns.get(0);
                        if (bestPattern.getInputData() != null) {
                            recommendations.put("recommendedProvider",
                                    bestPattern.getInputData().get("bestProvider"));
                            recommendations.put("recommendedPlatform",
                                    bestPattern.getInputData().get("bestPlatform"));
                        }
                    }

                    return recommendations;
                });
    }
}
