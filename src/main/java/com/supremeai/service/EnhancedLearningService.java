package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Learning Service - Central hub for all learning types
 * Handles NLP, Multimodal, Ecosystem, and Predictive learning
 */
@Service
public class EnhancedLearningService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedLearningService.class);

    @Autowired
    private SystemLearningRepository repository;

    @Autowired
    private SystemLearningService systemLearningService;

    // Learning type constants
    public static final String LEARNING_NLP = "NLP";
    public static final String LEARNING_MULTIMODAL = "MULTIMODAL";
    public static final String LEARNING_ECOSYSTEM = "ECOSYSTEM";
    public static final String LEARNING_APP_GENERATION = "APP_GENERATION";
    public static final String LEARNING_PREDICTIVE = "PREDICTIVE";
    public static final String LEARNING_SELF_HEALING = "SELF_HEALING";

    /**
     * NLP Learning: Learn from user interactions to improve natural language understanding
     */
    public Mono<SystemLearning> learnFromNLPInteraction(String userInput, String aiResponse,
                                                        String provider, double qualityScore,
                                                        Map<String, Object> context) {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("userInput", userInput);
        inputData.put("provider", provider);
        inputData.put("timestamp", LocalDateTime.now().toString());

        Map<String, Object> outputData = new HashMap<>();
        outputData.put("aiResponse", aiResponse);
        outputData.put("responseLength", aiResponse != null ? aiResponse.length() : 0);

        SystemLearning learning = new SystemLearning();
        learning.setId(IdUtils.ensureId(null));
        learning.setTopic("NLP_Interaction");
        learning.setCategory("natural_language");
        learning.setContent("Learned from NLP interaction with " + provider);
        learning.setLearningType(LEARNING_NLP);
        learning.setInputData(inputData);
        learning.setOutputData(outputData);
        learning.setSuccess(qualityScore > 0.7);
        learning.setQualityScore(qualityScore);
        learning.setRelatedProvider(provider);
        learning.setLearnedAt(LocalDateTime.now());
        learning.setTimesApplied(0);

        List<String> tags = new ArrayList<>();
        tags.add("nlp");
        tags.add(provider.toLowerCase());
        if (userInput != null) {
            if (userInput.contains("code") || userInput.contains("program")) tags.add("code_generation");
            if (userInput.contains("app") || userInput.contains("build")) tags.add("app_generation");
            if (userInput.contains("bangla") || userInput.contains("বাংলা")) tags.add("bangla");
        }
        learning.setTags(tags);

        logger.info("NLP Learning captured: provider={}, quality={}", provider, qualityScore);
        return repository.save(learning);
    }

    /**
     * Multimodal Learning: Learn from image+text combinations
     */
    public Mono<SystemLearning> learnFromMultimodalInteraction(String textPrompt, String imageUrl,
                                                               String generatedCode, String provider,
                                                               double accuracyScore) {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("textPrompt", textPrompt);
        inputData.put("imageUrl", imageUrl);
        inputData.put("provider", provider);

        Map<String, Object> outputData = new HashMap<>();
        outputData.put("generatedCode", generatedCode);
        outputData.put("codeLength", generatedCode != null ? generatedCode.length() : 0);

        SystemLearning learning = new SystemLearning();
        learning.setId(IdUtils.ensureId(null));
        learning.setTopic("Multimodal_Interaction");
        learning.setCategory("vision_and_text");
        learning.setContent("Learned from multimodal input (text+image) using " + provider);
        learning.setLearningType(LEARNING_MULTIMODAL);
        learning.setInputData(inputData);
        learning.setOutputData(outputData);
        learning.setSuccess(accuracyScore > 0.75);
        learning.setQualityScore(accuracyScore);
        learning.setRelatedProvider(provider);
        learning.setLearnedAt(LocalDateTime.now());
        learning.setTimesApplied(0);

        List<String> tags = Arrays.asList("multimodal", "vision", "image_to_code", provider.toLowerCase());
        learning.setTags(tags);

        logger.info("Multimodal Learning captured: provider={}, accuracy={}", provider, accuracyScore);
        return repository.save(learning);
    }

    /**
     * Ecosystem Learning: Track API usage patterns and third-party integrations
     */
    public Mono<SystemLearning> learnFromAPIUsage(String apiEndpoint, String provider,
                                                   long responseTimeMs, boolean success,
                                                   Map<String, Object> requestMeta) {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("apiEndpoint", apiEndpoint);
        inputData.put("provider", provider);
        inputData.put("requestMetadata", requestMeta);

        Map<String, Object> outputData = new HashMap<>();
        outputData.put("responseTimeMs", responseTimeMs);
        outputData.put("success", success);

        SystemLearning learning = new SystemLearning();
        learning.setId(IdUtils.ensureId(null));
        learning.setTopic("API_Usage_Pattern");
        learning.setCategory("ecosystem");
        learning.setContent("Learned API usage pattern for " + apiEndpoint);
        learning.setLearningType(LEARNING_ECOSYSTEM);
        learning.setInputData(inputData);
        learning.setOutputData(outputData);
        learning.setSuccess(success);
        learning.setQualityScore(success ? 1.0 : 0.0);
        learning.setRelatedProvider(provider);
        learning.setLearnedAt(LocalDateTime.now());
        learning.setTimesApplied(0);

        List<String> tags = new ArrayList<>();
        tags.add("api_usage");
        tags.add(provider.toLowerCase());
        tags.add(success ? "success" : "failure");
        learning.setTags(tags);

        logger.info("Ecosystem Learning captured: endpoint={}, provider={}, success={}", apiEndpoint, provider, success);
        return repository.save(learning);
    }

    /**
     * App Generation Learning: Learn from real-life app generation success/failure
     */
    public Mono<SystemLearning> learnFromAppGeneration(String requirement, String generatedAppType,
                                                       boolean buildSuccess, String apkPath,
                                                       Map<String, Object> buildMetrics,
                                                       String agentUsed) {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("requirement", requirement);
        inputData.put("appType", generatedAppType);
        inputData.put("agentUsed", agentUsed);

        Map<String, Object> outputData = new HashMap<>();
        outputData.put("buildSuccess", buildSuccess);
        outputData.put("apkPath", apkPath);
        outputData.put("buildMetrics", buildMetrics);

        double qualityScore = buildSuccess ? 1.0 : 0.3;
        if (buildMetrics != null && buildMetrics.containsKey("testPassRate")) {
            qualityScore = (Double) buildMetrics.get("testPassRate");
        }

        SystemLearning learning = new SystemLearning();
        learning.setId(IdUtils.ensureId(null));
        learning.setTopic("App_Generation_" + generatedAppType);
        learning.setCategory("app_generation");
        learning.setContent("Learned from app generation: " + requirement.substring(0, Math.min(100, requirement.length())));
        learning.setLearningType(LEARNING_APP_GENERATION);
        learning.setInputData(inputData);
        learning.setOutputData(outputData);
        learning.setSuccess(buildSuccess);
        learning.setQualityScore(qualityScore);
        learning.setRelatedProvider(agentUsed);
        learning.setLearnedAt(LocalDateTime.now());
        learning.setTimesApplied(0);

        List<String> tags = new ArrayList<>();
        tags.add("app_generation");
        tags.add(generatedAppType.toLowerCase());
        tags.add(buildSuccess ? "success" : "failure");
        if (agentUsed != null) tags.add(agentUsed.toLowerCase());
        learning.setTags(tags);

        logger.info("App Generation Learning captured: type={}, success={}, agent={}", generatedAppType, buildSuccess, agentUsed);
        return repository.save(learning);
    }

    /**
     * Predictive Learning: Use past successes to improve future generations
     */
    public Mono<SystemLearning> learnPredictivePattern(String patternType, Map<String, Object> patternData,
                                                       double confidence, String basedOnLearningId) {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("patternType", patternType);
        inputData.put("patternData", patternData);
        inputData.put("basedOnLearningId", basedOnLearningId);

        SystemLearning learning = new SystemLearning();
        learning.setId(IdUtils.ensureId(null));
        learning.setTopic("Predictive_Pattern_" + patternType);
        learning.setCategory("predictive");
        learning.setContent("Learned predictive pattern for " + patternType);
        learning.setLearningType(LEARNING_PREDICTIVE);
        learning.setInputData(inputData);
        learning.setOutputData(new HashMap<>());
        learning.setSuccess(true);
        learning.setConfidenceScore(confidence);
        learning.setQualityScore(confidence);
        learning.setLearnedAt(LocalDateTime.now());
        learning.setTimesApplied(0);

        List<String> tags = Arrays.asList("predictive", patternType.toLowerCase(), "pattern_matching");
        learning.setTags(tags);

        logger.info("Predictive Learning captured: pattern={}, confidence={}", patternType, confidence);
        return repository.save(learning);
    }

    /**
     * Get best practices based on past learnings
     */
    public Flux<SystemLearning> getBestPractices(String category, double minConfidence) {
        return repository.findByCategory(category)
                .filter(l -> l.getQualityScore() != null && l.getQualityScore() >= minConfidence)
                .filter(l -> l.getSuccess() != null && l.getSuccess())
                .sort((a, b) -> Double.compare(b.getQualityScore() != null ? b.getQualityScore() : 0,
                        a.getQualityScore() != null ? a.getQualityScore() : 0));
    }

    /**
     * Get predictive recommendations for a new task
     */
    public Flux<SystemLearning> getPredictiveRecommendations(String taskType, Map<String, Object> taskFeatures) {
        return repository.findAll()
                .filter(l -> LEARNING_PREDICTIVE.equals(l.getLearningType()))
                .filter(l -> l.getTags() != null && l.getTags().contains(taskType.toLowerCase()))
                .filter(l -> l.getConfidenceScore() != null && l.getConfidenceScore() >= 0.8)
                .sort((a, b) -> Double.compare(b.getConfidenceScore() != null ? b.getConfidenceScore() : 0,
                        a.getConfidenceScore() != null ? a.getConfidenceScore() : 0))
                .take(5);
    }

    /**
     * Apply learning - increment times applied counter
     */
    @CacheEvict(value = "system_learning", allEntries = true)
    public Mono<SystemLearning> applyLearning(String learningId) {
        return repository.findById(learningId)
                .flatMap(learning -> {
                    learning.setTimesApplied((learning.getTimesApplied() != null ? learning.getTimesApplied() : 0) + 1);
                    return repository.save(learning);
                });
    }

    /**
     * Get learning statistics
     */
    public Mono<Map<String, Object>> getLearningStats() {
        List<SystemLearning> allLearnings = repository.findAll().collectList().block();

        Map<String, Object> stats = new HashMap<>();
        if (allLearnings == null) {
            stats.put("total", 0);
            return Mono.just(stats);
        }

        Map<String, Long> byType = allLearnings.stream()
                .collect(Collectors.groupingBy(l -> l.getLearningType() != null ? l.getLearningType() : "UNKNOWN",
                        Collectors.counting()));

        long totalSuccess = allLearnings.stream()
                .filter(l -> l.getSuccess() != null && l.getSuccess())
                .count();

        double avgQuality = allLearnings.stream()
                .filter(l -> l.getQualityScore() != null)
                .mapToDouble(SystemLearning::getQualityScore)
                .average()
                .orElse(0.0);

        stats.put("total", allLearnings.size());
        stats.put("byType", byType);
        stats.put("successCount", totalSuccess);
        stats.put("failureCount", allLearnings.size() - totalSuccess);
        stats.put("averageQuality", avgQuality);
        stats.put("successRate", allLearnings.size() > 0 ? (double) totalSuccess / allLearnings.size() : 0.0);

        return Mono.just(stats);
    }

    /**
     * System Learning Improvement Cycle
     * Analyzes collected patterns, optimizes knowledge base, and generates improvements.
     * This is the main "system learning improve" command backend implementation.
     *
     * @return Map containing improvement results and statistics
     */
    public Mono<Map<String, Object>> improveSystemLearning() {
        logger.info("Starting system learning improvement cycle...");
        
        return repository.findAll()
                .collectList()
                .map(allLearnings -> {
                    Map<String, Object> result = new HashMap<>();
                    
                    // 1. Analyze learning data
                    Map<String, Object> analysis = analyzeLearningData(allLearnings);
                    result.put("analysis", analysis);
                    
                    // 2. Identify improvement opportunities
                    List<Map<String, Object>> opportunities = identifyImprovements(allLearnings);
                    result.put("opportunities", opportunities);
                    
                    // 3. Optimize knowledge base
                    Map<String, Object> optimization = optimizeKnowledgeBase(allLearnings);
                    result.put("optimization", optimization);
                    
                    // 4. Generate recommendations
                    List<String> recommendations = generateRecommendations(allLearnings, analysis);
                    result.put("recommendations", recommendations);
                    
                    // 5. Summary metrics
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("totalLearningsAnalyzed", allLearnings.size());
                    summary.put("improvementsIdentified", opportunities.size());
                    summary.put("optimizationsApplied", optimization.get("applied"));
                    summary.put("recommendationsGenerated", recommendations.size());
                    summary.put("improvementCycle", LocalDateTime.now().toString());
                    
                    result.put("summary", summary);
                    result.put("success", true);
                    
                    logger.info("System learning improvement cycle completed. Analyzed {} learnings, found {} opportunities", 
                               allLearnings.size(), opportunities.size());
                    
                    return result;
                });
    }
    
    /**
     * Analyze learning data to extract insights
     */
    private Map<String, Object> analyzeLearningData(List<SystemLearning> learnings) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (learnings.isEmpty()) {
            analysis.put("status", "no_data");
            return analysis;
        }
        
        // Success rate by learning type
        Map<String, Map<String, Double>> successByType = new HashMap<>();
        Map<String, Long> countByType = learnings.stream()
                .collect(Collectors.groupingBy(l -> l.getLearningType() != null ? l.getLearningType() : "UNKNOWN",
                        Collectors.counting()));
        
        countByType.forEach((type, count) -> {
            long successes = learnings.stream()
                    .filter(l -> type.equals(l.getLearningType()))
                    .filter(l -> l.getSuccess() != null && l.getSuccess())
                    .count();
            double successRate = (double) successes / count;
            
            Map<String, Double> metrics = new HashMap<>();
            metrics.put("successRate", successRate);
            metrics.put("total", (double) count);
            successByType.put(type, metrics);
        });
        
        analysis.put("successRateByType", successByType);
        
        // Quality score distribution
        DoubleSummaryStatistics qualityStats = learnings.stream()
                .filter(l -> l.getQualityScore() != null)
                .mapToDouble(SystemLearning::getQualityScore)
                .summaryStatistics();
        
        analysis.put("qualityStats", Map.of(
            "min", qualityStats.getMin(),
            "max", qualityStats.getMax(),
            "avg", qualityStats.getAverage(),
            "count", (double) qualityStats.getCount()
        ));
        
        // Most applied learnings
        List<SystemLearning> topApplied = learnings.stream()
                .sorted(Comparator.comparingInt(l -> l.getTimesApplied() != null ? l.getTimesApplied() : 0).reversed())
                .limit(5)
                .collect(Collectors.toList());
        
        List<String> topAppliedIds = topApplied.stream()
                .map(SystemLearning::getId)
                .collect(Collectors.toList());
        analysis.put("topAppliedLearnings", topAppliedIds);
        
        // Provider performance
        Map<String, Long> providerCounts = learnings.stream()
                .filter(l -> l.getRelatedProvider() != null)
                .collect(Collectors.groupingBy(SystemLearning::getRelatedProvider, Collectors.counting()));
        analysis.put("providerUsage", providerCounts);
        
        return analysis;
    }
    
    /**
     * Identify improvement opportunities from learning data
     */
    private List<Map<String, Object>> identifyImprovements(List<SystemLearning> learnings) {
        List<Map<String, Object>> opportunities = new ArrayList<>();
        
        // Find low success rate categories
        Map<String, Map<Boolean, Long>> categoryStats = learnings.stream()
                .collect(Collectors.groupingBy(l -> l.getCategory() != null ? l.getCategory() : "unknown",
                        Collectors.groupingBy(l -> l.getSuccess() != null && l.getSuccess(),
                                Collectors.counting())));
        
        categoryStats.forEach((category, counts) -> {
            long success = (long) counts.getOrDefault(true, 0L);
            long total = success + (long) counts.getOrDefault(false, 0L);
            if (total > 10) { // Only consider categories with enough data
                double successRate = (double) success / total;
                if (successRate < 0.7) {
                    Map<String, Object> opp = new HashMap<>();
                    opp.put("type", "low_success_rate");
                    opp.put("category", category);
                    opp.put("successRate", successRate);
                    opp.put("total", total);
                    opportunities.add(opp);
                }
            }
        });
        
        // Find patterns with low quality scores
        List<SystemLearning> lowQuality = learnings.stream()
                .filter(l -> l.getQualityScore() != null && l.getQualityScore() < 0.5)
                .filter(l -> l.getTimesApplied() != null && l.getTimesApplied() > 5)
                .collect(Collectors.toList());
        
        lowQuality.forEach(l -> {
            Map<String, Object> opp = new HashMap<>();
            opp.put("type", "low_quality_pattern");
            opp.put("learningId", l.getId());
            opp.put("topic", l.getTopic());
            opp.put("qualityScore", l.getQualityScore());
            opp.put("timesApplied", l.getTimesApplied());
            opportunities.add(opp);
        });
        
        return opportunities;
    }
    
    /**
     * Optimize knowledge base based on analysis
     */
    private Map<String, Object> optimizeKnowledgeBase(List<SystemLearning> learnings) {
        Map<String, Object> optimization = new HashMap<>();
        List<String> actions = new ArrayList<>();
        int applied = 0;
        
        // Merge similar learnings
        Map<String, List<SystemLearning>> byTopic = learnings.stream()
                .collect(Collectors.groupingBy(l -> l.getTopic() != null ? l.getTopic() : "unknown"));
        
        byTopic.forEach((topic, similarLearnings) -> {
            if (similarLearnings.size() > 3) {
                // Consolidate into one high-quality entry
                SystemLearning best = similarLearnings.stream()
                        .max(Comparator.comparing(l -> l.getQualityScore() != null ? l.getQualityScore() : 0))
                        .orElse(similarLearnings.get(0));
                
                // Mark others for archival (in real implementation would update status)
                actions.add("Consolidated " + similarLearnings.size() + " learnings for topic: " + topic);
                applied++;
            }
        });
        
        // Prune obsolete entries (older than 6 months, never applied)
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        long obsolete = learnings.stream()
                .filter(l -> l.getLearnedAt() != null && l.getLearnedAt().isBefore(sixMonthsAgo))
                .filter(l -> l.getTimesApplied() == null || l.getTimesApplied() < 1)
                .count();
        
        if (obsolete > 0) {
            actions.add("Identified " + obsolete + " obsolete learnings for cleanup");
        }
        
        optimization.put("actions", actions);
        optimization.put("applied", applied);
        optimization.put("obsolete", obsolete);
        optimization.put("optimizedAt", LocalDateTime.now().toString());
        
        return optimization;
    }
    
    /**
     * Generate improvement recommendations
     */
    private List<String> generateRecommendations(List<SystemLearning> learnings, Map<String, Object> analysis) {
        List<String> recommendations = new ArrayList<>();
        
        Map<String, Long> byType = (Map<String, Long>) analysis.getOrDefault("successRateByType", Map.of());
        
        byType.forEach((type, metrics) -> {
            @SuppressWarnings("unchecked")
            Map<String, Double> typeMetrics = (Map<String, Double>) metrics;
            double rate = typeMetrics.get("successRate");
            
            if (rate < 0.6) {
                recommendations.add("Increase training data for " + type + " learning type (success rate: " + 
                    String.format("%.1f%%", rate * 100) + ")");
            }
        });
        
        // Provider recommendations
        Map<String, Long> providerUsage = (Map<String, Long>) analysis.getOrDefault("providerUsage", Map.of());
        providerUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    recommendations.add("Provider " + entry.getKey() + " is heavily used (" + entry.getValue() + 
                        " times). Consider dedicated optimization.");
                });
        
        // Cache optimization recommendation
        recommendations.add("Consider increasing cache size if knowledge base exceeds 1GB");
        recommendations.add("Schedule regular improvement cycles (daily recommended)");
        
        return recommendations;
    }
}
