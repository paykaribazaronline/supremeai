package com.supremeai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks AI provider performance (success rates) and ranks them.
 * Uses actual request outcomes to provide auto-ranking.
 */
@Service
public class AIRankingService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "ai_provider_stats:";
    private final Map<String, ProviderStats> providerStats = new ConcurrentHashMap<>();

    /**
     * Record a successful request to a provider.
     */
    public void recordSuccess(String provider) {
        ProviderStats stats = providerStats.computeIfAbsent(provider, k -> new ProviderStats());
        stats.successCount++;
        persistStats(provider, stats);
    }

    /**
     * Record a failed request to a provider.
     */
    public void recordFailure(String provider) {
        ProviderStats stats = providerStats.computeIfAbsent(provider, k -> new ProviderStats());
        stats.failureCount++;
        persistStats(provider, stats);
    }

    private void persistStats(String provider, ProviderStats stats) {
        try {
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + provider, stats);
        } catch (Exception e) {
            // Keep in-memory as fallback
        }
    }

    /**
     * Record a request outcome.
     */
    public void recordRequest(String provider, boolean success) {
        if (success) {
            recordSuccess(provider);
        } else {
            recordFailure(provider);
        }
    }

    /**
     * Get ranked list of providers by success rate (highest first).
     */
    public List<ProviderRanking> getRankings() {
        // Sync with Redis if possible
        try {
            Set<String> keys = redisTemplate.keys(REDIS_KEY_PREFIX + "*");
            if (keys != null) {
                for (String key : keys) {
                    String provider = key.replace(REDIS_KEY_PREFIX, "");
                    ProviderStats stats = (ProviderStats) redisTemplate.opsForValue().get(key);
                    if (stats != null) {
                        providerStats.put(provider, stats);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to in-memory stats
        }

        List<ProviderRanking> rankings = new ArrayList<>();
        for (Map.Entry<String, ProviderStats> entry : providerStats.entrySet()) {
            ProviderStats stats = entry.getValue();
            double successRate = stats.getSuccessRate();
            rankings.add(new ProviderRanking(
                entry.getKey(),
                stats.successCount,
                stats.failureCount,
                successRate
            ));
        }
        // Sort by success rate descending
        rankings.sort((a, b) -> Double.compare(b.successRate, a.successRate));
        return rankings;
    }

    /**
     * Get stats for a specific provider.
     */
    public ProviderRanking getRankingForProvider(String provider) {
        ProviderStats stats = providerStats.get(provider);
        if (stats == null) {
            return new ProviderRanking(provider, 0, 0, 0.0);
        }
        return new ProviderRanking(provider, stats.successCount, stats.failureCount, stats.getSuccessRate());
    }

    private static class ProviderStats implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        int successCount = 0;
        int failureCount = 0;

        double getSuccessRate() {
            int total = successCount + failureCount;
            return total == 0 ? 0.0 : (successCount * 100.0) / total;
        }
    }

    public static class ProviderRanking {
        private final String provider;
        private final int successCount;
        private final int failureCount;
        private final double successRate;

        public ProviderRanking(String provider, int successCount, int failureCount, double successRate) {
            this.provider = provider;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.successRate = successRate;
        }

        public String getProvider() { return provider; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public double getSuccessRate() { return successRate; }
    }
}
