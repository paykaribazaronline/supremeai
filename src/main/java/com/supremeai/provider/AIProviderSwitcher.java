package com.supremeai.provider;

import com.supremeai.fallback.AIFallbackOrchestrator;
import com.supremeai.resilience.RetryableAIExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * প্রোভাইডার সুইচিং মেকানিজম যা ব্যবহারকারীর পছন্দ অনুযায়ী স্বয়ংক্রিয়ভাবে প্রোভাইডার পরিবর্তন করতে পারে।
 * এটি প্রতিটি প্রোভাইডারের পারফরম্যান্স ট্র্যাক করে এবং সেরা প্রোভাইডার নির্বাচন করে।
 */
@Service
public class AIProviderSwitcher {

    private static final Logger logger = LoggerFactory.getLogger(AIProviderSwitcher.class);

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private AIFallbackOrchestrator fallbackOrchestrator;

    @Autowired
    private RetryableAIExecutor retryExecutor;

    // প্রোভাইডার পারফরম্যান্স মেট্রিক্স
    private final Map<String, ProviderMetrics> providerMetrics = new ConcurrentHashMap<>();

    // প্রোভাইডার প্রায়োরিটি
    private final Map<String, Integer> providerPriority = new ConcurrentHashMap<>();

    // ব্যবহারকারী পছন্দ
    private final Map<String, String> userPreferredProviders = new ConcurrentHashMap<>();

    /**
     * প্রোভাইডার সুইচ করার জন্য মেথড
     */
    public String switchProvider(String currentProvider, String taskCategory, String prompt, String userId) {
        logger.info("Provider switch initiated for user: {}, current provider: {}, task: {}", userId, currentProvider, taskCategory);

        // ব্যবহারকারীর পছন্দ চেক করা
        String preferredProvider = userPreferredProviders.get(userId);
        if (preferredProvider != null && !preferredProvider.equals(currentProvider)) {
            logger.info("Switching to user preferred provider: {}", preferredProvider);
            return executeWithProvider(preferredProvider, taskCategory, prompt, userId);
        }

        // প্রোভাইডার পারফরম্যান্স বিশ্লেষণ করা
        String bestProvider = findBestProviderForTask(taskCategory);
        if (bestProvider != null && !bestProvider.equals(currentProvider)) {
            logger.info("Switching to best performing provider: {} for task: {}", bestProvider, taskCategory);
            return executeWithProvider(bestProvider, taskCategory, prompt, userId);
        }

        // ফলব্যাক অর্কেস্ট্রেটর ব্যবহার করা
        logger.info("Using fallback orchestrator for task: {}", taskCategory);
        return fallbackOrchestrator.executeWithSupremeIntelligence(taskCategory, "provider_switch", prompt, userId);
    }

    /**
     * নির্দিষ্ট প্রোভাইডার দিয়ে কাজ সম্পন্ন করা
     */
    private String executeWithProvider(String providerName, String taskCategory, String prompt, String userId) {
        try {
            AIProvider provider = providerFactory.getProvider(providerName);
            long startTime = System.currentTimeMillis();
            String result = provider.generate(prompt);
            long duration = System.currentTimeMillis() - startTime;

            // পারফরম্যান্স রেকর্ড করা
            recordProviderPerformance(providerName, taskCategory, true, duration);

            return result;
        } catch (Exception e) {
            logger.error("Error executing with provider {}: {}", providerName, e.getMessage());
            recordProviderPerformance(providerName, taskCategory, false, 0);

            // ফলব্যাক অর্কেস্ট্রেটর ব্যবহার করা
            return fallbackOrchestrator.executeWithSupremeIntelligence(taskCategory, "provider_error:" + providerName, prompt, userId);
        }
    }

    /**
     * কাজের জন্য সেরা প্রোভাইডার খুঁজে বের করা
     */
    private String findBestProviderForTask(String taskCategory) {
        List<ProviderMetrics> candidates = new ArrayList<>();

        for (Map.Entry<String, ProviderMetrics> entry : providerMetrics.entrySet()) {
            ProviderMetrics metrics = entry.getValue();
            if (metrics.getTaskCategory().equals(taskCategory) && metrics.getSuccessRate() > 0.7) {
                candidates.add(metrics);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // সাফল্য হার এবং গড় প্রতিক্রিয়া সময়ের ভিত্তিতে সাজানো
        candidates.sort((a, b) -> {
            double scoreA = a.getSuccessRate() / (a.getAverageResponseTime() + 1);
            double scoreB = b.getSuccessRate() / (b.getAverageResponseTime() + 1);
            return Double.compare(scoreB, scoreA);
        });

        return candidates.get(0).getProviderName();
    }

    /**
     * প্রোভাইডার পারফরম্যান্স রেকর্ড করা
     */
    public void recordProviderPerformance(String providerName, String taskCategory, boolean success, long responseTime) {
        providerMetrics.computeIfAbsent(providerName + ":" + taskCategory, 
            k -> new ProviderMetrics(providerName, taskCategory))
            .recordPerformance(success, responseTime);
    }

    /**
     * ব্যবহারকারীর পছন্দের প্রোভাইডার সেট করা
     */
    public void setUserPreferredProvider(String userId, String providerName) {
        userPreferredProviders.put(userId, providerName);
        logger.info("User {} preferred provider set to: {}", userId, providerName);
    }

    /**
     * প্রোভাইডার প্রায়োরিটি সেট করা
     */
    public void setProviderPriority(String providerName, int priority) {
        providerPriority.put(providerName, priority);
        logger.info("Provider {} priority set to: {}", providerName, priority);
    }

    /**
     * প্রোভাইডার মেট্রিক্স পাওয়া
     */
    public Map<String, Object> getProviderMetrics() {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, ProviderMetrics> entry : providerMetrics.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getMetrics());
        }
        return result;
    }

    /**
     * প্রোভাইডার মেট্রিক্স ক্লাস
     */
    private static class ProviderMetrics {
        private final String providerName;
        private final String taskCategory;
        private final AtomicInteger totalRequests = new AtomicInteger(0);
        private final AtomicInteger successfulRequests = new AtomicInteger(0);
        private final AtomicInteger totalResponseTime = new AtomicInteger(0);
        private final long createdAt = System.currentTimeMillis();

        public ProviderMetrics(String providerName, String taskCategory) {
            this.providerName = providerName;
            this.taskCategory = taskCategory;
        }

        public void recordPerformance(boolean success, long responseTime) {
            totalRequests.incrementAndGet();
            if (success) {
                successfulRequests.incrementAndGet();
                totalResponseTime.addAndGet((int) responseTime);
            }
        }

        public double getSuccessRate() {
            int total = totalRequests.get();
            return total > 0 ? (double) successfulRequests.get() / total : 0;
        }

        public double getAverageResponseTime() {
            int successful = successfulRequests.get();
            return successful > 0 ? (double) totalResponseTime.get() / successful : 0;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getTaskCategory() {
            return taskCategory;
        }

        public Map<String, Object> getMetrics() {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("providerName", providerName);
            metrics.put("taskCategory", taskCategory);
            metrics.put("totalRequests", totalRequests.get());
            metrics.put("successfulRequests", successfulRequests.get());
            metrics.put("successRate", getSuccessRate());
            metrics.put("averageResponseTime", getAverageResponseTime());
            metrics.put("createdAt", createdAt);
            return metrics;
        }
    }
}
