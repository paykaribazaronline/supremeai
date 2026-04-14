package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @deprecated Use {@link UnifiedAIRoutingService} instead.
 * AI Provider Routing Service
 * Intelligently routes requests to best available AI provider based on:
 * - Available quota
 * - Success rate
 * - Response time history
 * - Category-specific affinity (learned over time)
 */
@Service
public class AIProviderRoutingService {
    private static final Logger logger = LoggerFactory.getLogger(AIProviderRoutingService.class);
    
    @Autowired
    private QuotaService quotaService;
    
    /**
     * Performance metrics per provider per category
     */
    public static class ProviderMetrics {
        public String provider;
        public String category;  // "architecture", "coding", "error_handling", etc
        public int successCount = 0;
        public int failureCount = 0;
        public long totalResponseTimeMs = 0;
        public double qualityScore = 0.5;  // 0-1, starts neutral
        
        public double getSuccessRate() {
            int total = successCount + failureCount;
            return total == 0 ? 0.5 : (double) successCount / total;
        }
        
        public double getAverageResponseTime() {
            return successCount == 0 ? 0 : totalResponseTimeMs / (double) successCount;
        }
        
        public void recordSuccess(long responseTimeMs, double qualityScore) {
            successCount++;
            totalResponseTimeMs += responseTimeMs;
            // Exponential moving average for quality
            this.qualityScore = 0.7 * this.qualityScore + 0.3 * qualityScore;
        }
        
        public void recordFailure() {
            failureCount++;
        }
    }
    
    private final Map<String, ProviderMetrics> metricsMap = new ConcurrentHashMap<>();
    
    /**
     * Route request to best provider
     * Strategy:
     * 1. Check quota availability
     * 2. Select by category affinity (learned)
     * 3. Fallback to highest success rate
     * 4. Final fallback to round-robin
     */
    public String routeRequest(String category, String requestType) {
        // First priority: Use category-affine provider with available quota
        String categoryOptimal = getCategoryOptimalProvider(category);
        if (categoryOptimal != null) {
            logger.info("🎯 Routing {} request ({}) to {}", requestType, category, categoryOptimal);
            return categoryOptimal;
        }
        
        // Second priority: Optimal provider (highest quota + success rate)
        List<String> available = quotaService.getAvailableProviders();
        if (available.isEmpty()) return null;
        
        String optimal = available.stream()
            .max(Comparator.comparingDouble(p -> quotaService.getRemainingQuotaPercent(p)))
            .orElse(available.get(0));
            
        logger.info("🎯 Routing {} request to optimal: {}", requestType, optimal);
        return optimal;
    }
    
    /**
     * Get best provider for specific category based on learned performance
     */
    private String getCategoryOptimalProvider(String category) {
        return metricsMap.entrySet().stream()
            .filter(e -> e.getValue().category.equals(category) && e.getValue().getSuccessRate() > 0.7)
            .max(Comparator.comparingDouble(e -> e.getValue().getSuccessRate()))
            .map(e -> e.getValue().provider)
            .orElse(null);
    }
    
    /**
     * Record provider performance for learning
     */
    public void recordProviderPerformance(
            String provider,
            String category,
            boolean success,
            long responseTimeMs,
            double qualityScore) {
        
        String metricsKey = provider + "_" + category;
        ProviderMetrics metrics = metricsMap.computeIfAbsent(metricsKey, k ->
            new ProviderMetrics() {{
                ProviderMetrics thisVar = this;
                thisVar.provider = provider;
                thisVar.category = category;
            }}
        );
        
        if (success) {
            metrics.recordSuccess(responseTimeMs, qualityScore);
            quotaService.recordUsage(provider, 200);
            logger.info("✅ {} performance: {}ms, quality: {}, success_rate: {:.1f}%",
                provider, responseTimeMs, qualityScore, metrics.getSuccessRate() * 100);
        } else {
            metrics.recordFailure();
            // QuotaService doesn't record failures in the same way, 
            // but we could record a 0-token usage if needed to trigger sync/checks
            logger.warn("❌ {} failed for category: {}", provider, category);
        }
    }
    
    /**
     * Get provider recommendations for a category
     */
    public List<Map<String, Object>> getCategoryRecommendations(String category) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        metricsMap.entrySet().stream()
            .filter(e -> e.getValue().category.equals(category))
            .sorted((a, b) -> Double.compare(b.getValue().getSuccessRate(), a.getValue().getSuccessRate()))
            .forEach(e -> {
                ProviderMetrics metrics = e.getValue();
                recommendations.add(new LinkedHashMap<String, Object>() {{
                    put("provider", metrics.provider);
                    put("success_rate", String.format("%.1f%%", metrics.getSuccessRate() * 100));
                    put("attempts", metrics.successCount + metrics.failureCount);
                    put("avg_response_ms", Math.round(metrics.getAverageResponseTime()));
                    put("quality_score", String.format("%.2f", metrics.qualityScore));
                }});
            });
        
        return recommendations;
    }
    
    /**
     * Get all provider metrics
     */
    public Map<String, List<Map<String, Object>>> getAllMetrics() {
        Map<String, List<Map<String, Object>>> categoryMetrics = new TreeMap<>();
        
        metricsMap.values().stream()
            .collect(() -> new TreeMap<String, List<ProviderMetrics>>(),
                (map, metrics) -> map.computeIfAbsent(metrics.category, k -> new ArrayList<>()).add(metrics),
                (m1, m2) -> m2.forEach((k, v) -> m1.computeIfAbsent(k, k2 -> new ArrayList<>()).addAll(v)))
            .forEach((category, metricsList) -> {
                List<Map<String, Object>> categoryList = new ArrayList<>();
                
                metricsList.stream()
                    .sorted(Comparator.comparingDouble(ProviderMetrics::getSuccessRate).reversed())
                    .forEach(metrics -> {
                        categoryList.add(new LinkedHashMap<String, Object>() {{
                            put("provider", metrics.provider);
                            put("success_rate", String.format("%.1f%%", metrics.getSuccessRate() * 100));
                            put("successes", metrics.successCount);
                            put("failures", metrics.failureCount);
                            put("avg_response_ms", metrics.successCount > 0 ? 
                                Math.round(metrics.getAverageResponseTime()) : 0);
                            put("quality_score", String.format("%.2f", metrics.qualityScore));
                        }});
                    });
                
                categoryMetrics.put(category, categoryList);
            });
        
        return categoryMetrics;
    }
}
